// Aviso de error o de exito. Si no hay texto, no se muestra nada.
export default function Message({ type = 'error', text, onClose }) {
  if (!text) return null

  return (
    <div className={`message message-${type}`} role="alert">
      <span>{text}</span>
      <button type="button" className="close-button" onClick={onClose} aria-label="Cerrar mensaje">
        &times;
      </button>
    </div>
  )
}
