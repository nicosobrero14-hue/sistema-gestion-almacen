import { useState } from 'react'
import { editUser, saveUser } from '../api/users'
import Message from '../components/Message'
import Modal from '../components/Modal'
import TextField from '../components/TextField'

// EMPLEADO por defecto: es el rol con menos permisos.
const EMPTY = { name: '', lastName: '', username: '', password: '', role: 'EMPLEADO' }

// Alta y modificacion de usuarios (CU-18). Si user es null, es un alta.
// Al editar, la contraseña arranca vacia y solo se manda si se escribe una nueva.
export default function UserForm({ user, onSaved, onCancel }) {
  const [data, setData] = useState(user ? { ...user, password: '' } : EMPTY)
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  // Un solo manejador para todos los campos: usa el "name" del input.
  const onChange = (event) => setData({ ...data, [event.target.name]: event.target.value })

  const onSubmit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    setError('')

    const body = {
      name: data.name,
      lastName: data.lastName,
      username: data.username,
      password: data.password || null, // null le indica al backend que conserve la contraseña actual
      role: data.role,
    }

    try {
      if (user) {
        await editUser(user.id, body)
        onSaved(`"${body.username}" fue modificado.`)
      } else {
        await saveUser(body)
        onSaved(`"${body.username}" fue creado.`)
      }
    } catch (e) {
      setErrors(e.errors)
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={user ? `Editar usuario: ${user.username}` : 'Nuevo usuario'} onClose={onCancel}>
      <Message text={error} onClose={() => setError('')} />

      <form onSubmit={onSubmit} noValidate>
        <div className="fields-row">
          <TextField name="name" label="Nombre" value={data.name} onChange={onChange}
            error={errors.name} required maxLength={80} autoFocus />
          <TextField name="lastName" label="Apellido" value={data.lastName} onChange={onChange}
            error={errors.lastName} required maxLength={80} />
        </div>

        <TextField name="username" label="Nombre de usuario" value={data.username} onChange={onChange}
          error={errors.username} required maxLength={50} help="Letras, números, punto, guion y guion bajo" />

        {/* autoComplete: evita que el navegador complete la contraseña de quien esta usando el sistema. */}
        <TextField name="password" label={user ? 'Nueva contraseña' : 'Contraseña'} type="password"
          value={data.password} onChange={onChange} error={errors.password} required={!user}
          autoComplete="new-password"
          help={user ? 'Dejar vacío para conservar la actual' : 'Mínimo 8 caracteres, con letras y números'} />

        <div className="field">
          <label htmlFor="role">
            Rol<span className="required"> *</span>
          </label>
          <select id="role" name="role" value={data.role} onChange={onChange}>
            <option value="EMPLEADO">Empleado</option>
            <option value="ADMIN">Administrador</option>
          </select>
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
