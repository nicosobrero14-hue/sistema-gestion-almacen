// Campo de formulario con su etiqueta y el error del backend debajo (CU-02 exc. 5a).
// "name" tiene que ser igual al campo del DTO, porque es la clave con la que llega el error.
export default function TextField({ name, label, value, onChange, error, required = false, help, type = 'text', ...rest }) {
  return (
    <div className="field">
      <label htmlFor={name}>
        {label}
        {required && <span className="required"> *</span>}
      </label>

      <input
        id={name}
        name={name}
        type={type}
        value={value ?? ''}
        onChange={onChange}
        className={error ? 'has-error' : ''}
        aria-invalid={Boolean(error)}
        {...rest}
      />

      {help && !error && <small className="help">{help}</small>}
      {error && <small className="field-error">{error}</small>}
    </div>
  )
}
