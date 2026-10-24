// Lectura del QR de una factura electronica (RF-15 / CU-23).
//
// Desde 2021 todo comprobante electronico lleva impreso un QR de ARCA, el ex AFIP. Lo que
// contiene es una direccion con los datos adentro, en el parametro p, codificados en base64:
//
//   https://www.afip.gob.ar/fe/qr/?p=eyJ2ZXIiOjEsImZlY2hhIjoiMjAyNi0xMC0xNSIsLi4u
//
// Y adentro, un JSON con los campos de la resolucion general 4892: ver, fecha, cuit, ptoVta,
// tipoCmp, nroCmp, importe, moneda, ctz, tipoDocRec, nroDocRec, tipoCodAut y codAut.
//
// El escaner del comercio lee codigos 2D y escribe lo que lee como si fuera un teclado, igual
// que con los codigos de barras de los productos. Asi que alcanza con mirar lo que entra en el
// campo: si es uno de estos codigos, se completan los datos del comprobante.

const DIRECCION = 'afip.gob.ar/fe/qr'

// El CUIT viene como numero. Se guarda como lo imprime la factura: 30-71234567-8.
const formatCuit = (cuit) => {
  const texto = String(cuit)
  if (texto.length !== 11) {
    return texto
  }
  return `${texto.slice(0, 2)}-${texto.slice(2, 10)}-${texto.slice(10)}`
}

// base64 a texto, respetando los acentos. atob devuelve los bytes de a uno como caracteres,
// asi que hay que rearmarlos y decodificarlos como UTF-8.
const decodeBase64 = (value) => {
  const binario = atob(value)
  const bytes = Uint8Array.from(binario, (caracter) => caracter.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

// Devuelve los datos del comprobante, o null si el texto no es un QR de ARCA. Null es lo
// normal: por el campo pasa cualquier cosa que se escriba o se escanee.
export const readAfipQr = (text) => {
  if (!text || !text.includes(DIRECCION)) {
    return null
  }

  try {
    const direccion = new URL(text.trim())
    const parametro = direccion.searchParams.get('p')
    if (!parametro) {
      return null
    }

    const datos = JSON.parse(decodeBase64(parametro))
    if (!datos.nroCmp || !datos.ptoVta) {
      return null
    }

    return {
      // La moneda no se carga en ningun campo, pero la pantalla avisa si no son pesos: el
      // importe de un comprobante en dolares no se puede sumar a lo que se le debe al proveedor.
      currency: datos.moneda ?? 'PES',
      fields: {
        issuerCuit: datos.cuit ? formatCuit(datos.cuit) : '',
        voucherType: datos.tipoCmp ?? '',
        salesPoint: datos.ptoVta ?? '',
        number: datos.nroCmp ?? '',
        date: datos.fecha ?? '',
        invoicedTotal: datos.importe ?? '',
        authorizationCode: datos.codAut ? String(datos.codAut) : '',
      },
    }
  } catch (e) {
    // Un QR de otra cosa, o uno leido a medias. No es un error del sistema: se sigue cargando
    // la factura a mano, que es como se hacia antes.
    return null
  }
}
