import { useEffect, useRef } from 'react'

// Ventana que se abre sobre la pantalla. Se cierra con Escape, con la X o tocando el fondo.
// wide: mas ancha, para las tablas con muchas columnas.
export default function Modal({ title, onClose, wide = false, children }) {
  // Si el clic empezo en el fondo o adentro de la ventana.
  //
  // Hace falta porque al seleccionar texto con el mouse es normal pasarse del borde: se aprieta
  // adentro y se suelta afuera. El navegador considera eso un clic del fondo, y la ventana se
  // cerraba en medio de la carga, perdiendo lo que se estaba escribiendo.
  const empezoEnElFondo = useRef(false)

  // Mientras esta abierta, la tecla Escape la cierra.
  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  return (
    <div
      className="modal-background"
      onMouseDown={(event) => { empezoEnElFondo.current = event.target === event.currentTarget }}
      onClick={(event) => {
        // Las dos condiciones: que el clic termine en el fondo y que tambien haya empezado ahi.
        if (event.target === event.currentTarget && empezoEnElFondo.current) {
          onClose()
        }
      }}
    >
      <div
        className={wide ? 'modal modal-wide' : 'modal'}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <div className="modal-header">
          <h2 id="modal-title">{title}</h2>
          <button type="button" className="close-button" onClick={onClose} aria-label="Cerrar">
            &times;
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
