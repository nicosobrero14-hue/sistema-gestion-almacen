import { useEffect } from 'react'

// Ventana que se abre sobre la pantalla. Se cierra con Escape, con la X o tocando el fondo.
export default function Modal({ title, onClose, children }) {
  // Mientras esta abierta, la tecla Escape la cierra.
  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  return (
    <div className="modal-background" onClick={onClose}>
      {/* stopPropagation: un clic adentro no llega al fondo, asi no se cierra mientras se completa. */}
      <div
        className="modal"
        onClick={(event) => event.stopPropagation()}
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
