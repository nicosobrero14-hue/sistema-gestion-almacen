// Foto de un producto (RF-14 / CU-22).
// Sin foto muestra un recuadro con la inicial: ningun comercio carga las imagenes de todo
// el catalogo el primer dia, asi que la fila tiene que verse bien igual.
// src: la direccion de la imagen, ya armada por quien lo usa. Vacio es "todavia no tiene".
export default function ProductImage({ src, name, large = false }) {
  const size = large ? 'product-image product-image-large' : 'product-image'

  if (!src) {
    // aria-hidden: la inicial no aporta nada a quien usa un lector de pantalla, el nombre ya esta al lado.
    return <span className={`${size} product-image-empty`} aria-hidden="true">{(name ?? '?').trim().charAt(0).toUpperCase()}</span>
  }
  return <img className={size} src={src} alt={`Foto de ${name}`} />
}
