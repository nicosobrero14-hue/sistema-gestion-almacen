// Aviso de error, de exito o de advertencia. Si no hay texto, no se muestra nada.
// children: acciones opcionales, por ejemplo un boton para dar de alta un producto.
export default function Message({ type = 'error', text, onClose, children }) {
  if (!text) return null

  return (
    <div className={`message message-${type}`} role="alert">
      <span className="message-text">{text}</span>
      {children}
      <button type="button" className="close-button" onClick={onClose} aria-label="Cerrar mensaje">
        &times;
      </button>
    </div>
  )
}
