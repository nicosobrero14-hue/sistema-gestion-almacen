import { useState } from 'react'
import { login } from '../api/auth'
import Message from '../components/Message'
import TextField from '../components/TextField'

// Pantalla de inicio de sesion (CU-01).
export default function Login({ onLogin }) {
  const [data, setData] = useState({ username: '', password: '' })
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)

  // Un solo manejador para los dos campos: usa el "name" del input.
  const onChange = (event) => setData({ ...data, [event.target.name]: event.target.value })

  const onSubmit = async (event) => {
    event.preventDefault()
    setSending(true)
    setErrors({})
    setError('')

    try {
      onLogin(await login(data.username, data.password))
    } catch (e) {
      // Campos vacios (CU-01 exc. 3a): error en cada campo. Datos incorrectos (exc. 4a): mensaje general.
      setErrors(e.errors)
      if (Object.keys(e.errors).length === 0) setError(e.message)
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={onSubmit} noValidate>
        <div className="brand">
          <span className="brand-icon" aria-hidden="true">📦</span>
          <div>
            <strong>Gestión de Almacén</strong>
            <small>Control de stock</small>
          </div>
        </div>

        <h1>Iniciar sesión</h1>

        <Message text={error} onClose={() => setError('')} />

        <TextField name="username" label="Usuario" value={data.username} onChange={onChange}
          error={errors.username} required autoComplete="username" autoFocus />

        <TextField name="password" label="Contraseña" type="password" value={data.password}
          onChange={onChange} error={errors.password} required autoComplete="current-password" />

        <button type="submit" className="button button-primary button-wide" disabled={sending}>
          {sending ? 'Ingresando...' : 'Ingresar'}
        </button>
      </form>
    </div>
  )
}
