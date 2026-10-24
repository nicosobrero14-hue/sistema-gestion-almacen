import { useEffect, useRef, useState } from 'react'
import { savePurchase, uploadPurchaseImage } from '../api/purchases'
import { getSuppliers } from '../api/suppliers'
import Message from '../components/Message'
import Modal from '../components/Modal'
import TextField from '../components/TextField'
import { readAfipQr } from '../utils/afip'
import { VOUCHER_TYPES } from '../utils/format'

const EMPTY = {
  supplierId: '',
  issuerCuit: '',
  voucherType: 6, // factura B, que es la que recibe un comercio que compra como consumidor final
  salesPoint: '',
  number: '',
  date: '',
  invoicedTotal: '',
  agreedTotal: '',
  adjustmentReason: '',
  authorizationCode: '',
}

const toNumber = (value) => (value === '' ? null : Number(value))

// Alta de la factura de un proveedor (RF-15 / CU-23).
// onSaved recibe el mensaje para mostrar arriba del listado.
export default function PurchaseForm({ onSaved, onCancel }) {
  const [data, setData] = useState(EMPTY)
  const [suppliers, setSuppliers] = useState([])
  const [file, setFile] = useState(null)
  const [filePreview, setFilePreview] = useState('')
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  // El total acordado solo se pide cuando no se paga el total de la factura.
  const [corregido, setCorregido] = useState(false)

  // CU-23 paso 2: lo que lee el escaner del QR, y el aviso de como salio.
  const qrRef = useRef(null)
  const [qr, setQr] = useState('')
  const [qrMessage, setQrMessage] = useState('')

  useEffect(() => {
    getSuppliers('', true)
      .then(setSuppliers)
      .catch((e) => setError(e.message))
  }, [])

  // La vista previa de un archivo del disco es una direccion temporal que hay que liberar.
  useEffect(() => {
    if (!file) {
      setFilePreview('')
      return undefined
    }
    const temporal = URL.createObjectURL(file)
    setFilePreview(temporal)
    return () => URL.revokeObjectURL(temporal)
  }, [file])

  const onChange = (event) => {
    const { name, value } = event.target
    setData({ ...data, [name]: value })
  }

  // El escaner escribe lo que lee como si fuera un teclado, igual que con los codigos de barras.
  // Se mira cada vez que cambia el campo: cuando lo que entro es un QR de ARCA, se completan
  // los datos del comprobante y el campo queda libre para el siguiente.
  const onQrChange = (event) => {
    const texto = event.target.value
    setQr(texto)

    const leido = readAfipQr(texto)
    if (!leido) {
      return
    }

    // Si el CUIT del emisor es el de un proveedor cargado, se elige solo.
    const proveedor = suppliers.find((supplier) => supplier.cuit === leido.fields.issuerCuit)

    setData((actual) => ({
      ...actual,
      ...leido.fields,
      supplierId: proveedor ? proveedor.id : actual.supplierId,
    }))
    setQr('')
    setErrors({})
    setQrMessage(leido.currency === 'PES'
      ? 'Datos cargados desde el QR. Controle el total contra el papel antes de guardar.'
      : `El comprobante está en ${leido.currency}: el total se cargó tal cual viene en el QR.`)
  }

  const onFileChange = (event) => {
    const elegido = event.target.files[0]
    if (!elegido) return
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(elegido.type)) {
      setError('La foto tiene que ser JPG, PNG o WEBP.')
      return
    }
    if (elegido.size > 2 * 1024 * 1024) {
      setError('La foto no puede superar los 2 MB.')
      return
    }
    setError('')
    setFile(elegido)
  }

  const onSubmit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    setError('')

    const body = {
      supplierId: toNumber(data.supplierId),
      issuerCuit: data.issuerCuit || null,
      voucherType: toNumber(data.voucherType),
      salesPoint: toNumber(data.salesPoint),
      number: toNumber(data.number),
      date: data.date || null,
      invoicedTotal: toNumber(data.invoicedTotal),
      // Si no se corrigió, lo acordado es lo facturado y lo resuelve el servidor.
      agreedTotal: corregido ? toNumber(data.agreedTotal) : null,
      adjustmentReason: corregido ? data.adjustmentReason || null : null,
      authorizationCode: data.authorizationCode || null,
    }

    try {
      const saved = await savePurchase(body)
      let aviso = `Factura ${saved.voucherNumber} cargada.`

      // La foto viaja aparte: el nombre del archivo sale del id, que recién existe una vez guardada.
      try {
        if (file) await uploadPurchaseImage(saved.id, file)
      } catch (e) {
        aviso += ` La foto no se pudo guardar: ${e.message}`
      }
      onSaved(aviso)
    } catch (e) {
      setErrors(e.errors)
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title="Cargar factura de proveedor" onClose={onCancel}>
      <Message text={error} onClose={() => setError('')} />

      <form onSubmit={onSubmit} noValidate>
        {/* CU-23 paso 2: el QR impreso en el comprobante trae el CUIT del emisor, el punto de
            venta, el numero, la fecha, el importe y el CAE. Escanearlo evita copiar seis campos
            a mano y, sobre todo, evita equivocarse en el numero de factura. */}
        <div className="field">
          <label htmlFor="qr">Escanear el QR del comprobante</label>
          <input
            id="qr"
            ref={qrRef}
            type="text"
            value={qr}
            onChange={onQrChange}
            onKeyDown={(event) => { if (event.key === 'Enter') event.preventDefault() }}
            placeholder="Apunte el lector al código QR de la factura"
            autoFocus
          />
          <small className={qrMessage ? 'help scanned' : 'help'}>
            {qrMessage || 'Opcional. También se puede cargar todo a mano.'}
          </small>
        </div>

        <div className="field">
          <label htmlFor="supplierId">
            Proveedor
            <span className="required"> *</span>
          </label>
          <select id="supplierId" name="supplierId" value={data.supplierId} onChange={onChange}>
            <option value="">Elegir proveedor</option>
            {suppliers.map((supplier) => (
              <option key={supplier.id} value={supplier.id}>
                {supplier.name} {supplier.lastName ?? ''}
              </option>
            ))}
          </select>
          {errors.supplierId && <small className="field-error">{errors.supplierId}</small>}
        </div>

        <div className="fields-row">
          <div className="field">
            <label htmlFor="voucherType">Tipo de comprobante</label>
            <select id="voucherType" name="voucherType" value={data.voucherType} onChange={onChange}>
              {Object.keys(VOUCHER_TYPES).map((code) => (
                <option key={code} value={code}>{VOUCHER_TYPES[code]}</option>
              ))}
            </select>
          </div>
          <TextField name="salesPoint" label="Punto de venta" type="number" min="1" value={data.salesPoint}
            onChange={onChange} error={errors.salesPoint} required help="Los cuatro primeros dígitos" />
          <TextField name="number" label="Número" type="number" min="1" value={data.number}
            onChange={onChange} error={errors.number} required />
        </div>

        <div className="fields-row">
          <TextField name="date" label="Fecha del comprobante" type="date" value={data.date}
            onChange={onChange} error={errors.date} required />
          <TextField name="invoicedTotal" label="Total facturado" type="number" step="0.01" min="0"
            value={data.invoicedTotal} onChange={onChange} error={errors.invoicedTotal} required
            help="El que figura en el papel" />
        </div>

        {/* El caso de la factura con un renglón tachado: se facturó una cosa y se pagó otra. */}
        <label className="checkbox">
          <input type="checkbox" checked={corregido} onChange={(event) => setCorregido(event.target.checked)} />
          Se acordó pagar menos que el total facturado
        </label>

        {corregido && (
          <div className="fields-row">
            <TextField name="agreedTotal" label="Total acordado" type="number" step="0.01" min="0"
              value={data.agreedTotal} onChange={onChange} error={errors.agreedTotal} required
              help="Lo que realmente se debe" />
            <TextField name="adjustmentReason" label="Motivo" value={data.adjustmentReason}
              onChange={onChange} error={errors.adjustmentReason} required maxLength={255}
              help="Por ejemplo: faltó un producto del pedido" />
          </div>
        )}

        <div className="fields-row">
          <TextField name="issuerCuit" label="CUIT del emisor" value={data.issuerCuit} onChange={onChange}
            error={errors.issuerCuit} maxLength={13} help="Opcional" />
          <TextField name="authorizationCode" label="CAE" value={data.authorizationCode} onChange={onChange}
            error={errors.authorizationCode} maxLength={20} help="Opcional" />
        </div>

        <div className="image-field">
          {filePreview
            ? <img className="voucher-preview" src={filePreview} alt="Foto del comprobante" />
            : <span className="voucher-preview voucher-preview-empty" aria-hidden="true">📄</span>}
          <div>
            <label htmlFor="image">Foto del comprobante</label>
            <input id="image" type="file" accept="image/jpeg,image/png,image/webp" onChange={onFileChange} />
            <small className="help">JPG, PNG o WEBP de hasta 2 MB. Opcional, pero conviene.</small>
            {file && (
              <button type="button" className="button button-small button-secondary" onClick={() => setFile(null)}>
                Quitar foto
              </button>
            )}
          </div>
        </div>

        <div className="form-actions">
          <button type="button" className="button button-secondary" onClick={onCancel}>Cancelar</button>
          <button type="submit" className="button button-primary" disabled={saving}>
            {saving ? 'Guardando...' : 'Guardar'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
