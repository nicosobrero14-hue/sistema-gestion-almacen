import { useState } from 'react'
import { productThumbUrl } from '../api/products'

// Foto de un producto (RF-14 / CU-22).
// Sin foto muestra un recuadro con la inicial: ningun comercio carga las imagenes de todo
// el catalogo el primer dia, asi que la fila tiene que verse bien igual.
//
// image: el nombre del archivo tal como lo guarda la base. Vacio es "todavia no tiene".
// preview: una direccion ya armada, para la vista previa de un archivo que todavia no se subio.
// large: la foto grande de la ficha y del pesaje.
//
// En los listados se pide la miniatura y no la original: la original es de 600x600 y en la tabla
// se ve a 40x40, asi que el navegador decodificaria seis veces mas pixeles de los que se ven, por
// cada producto. En una computadora de mostrador vieja eso se nota.
export default function ProductImage({ image, preview, name, large = false }) {
  // Si la miniatura no esta, se usa la original. Pasa con las fotos cargadas antes de que el
  // sistema las hiciera, y con los formatos que el servidor no sabe achicar.
  const [sinMiniatura, setSinMiniatura] = useState(false)

  const original = preview || (image ? `/imagenes/${image}` : '')
  if (!original) {
    // aria-hidden: la inicial no aporta nada a quien usa un lector de pantalla, el nombre ya esta al lado.
    return (
      <span className={large ? 'product-image product-image-large product-image-empty' : 'product-image product-image-empty'}
        aria-hidden="true">
        {(name ?? '?').trim().charAt(0).toUpperCase()}
      </span>
    )
  }

  // La grande se muestra de a una, asi que no se gana nada con la miniatura.
  const usaMiniatura = !large && !preview && !sinMiniatura
  const src = usaMiniatura ? productThumbUrl(image) : original

  // loading lazy: en la grilla de la venta hay muchas fotos y solo se bajan las que se ven.
  return (
    <img
      className={large ? 'product-image product-image-large' : 'product-image'}
      src={src}
      alt={`Foto de ${name}`}
      loading="lazy"
      onError={() => setSinMiniatura(true)}
    />
  )
}
