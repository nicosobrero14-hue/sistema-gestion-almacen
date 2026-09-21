import { useEffect, useState } from 'react'
import { activateUser, deactivateUser, getUsers } from '../api/users'
import Message from '../components/Message'
import UserForm from './UserForm'

// Pantalla de usuarios (RF-11 / CU-18).
export default function Users() {
  const [users, setUsers] = useState([])
  const [search, setSearch] = useState('')
  const [activeOnly, setActiveOnly] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Formulario: abierto o cerrado, y el usuario que se edita (null = usuario nuevo).
  const [formOpen, setFormOpen] = useState(false)
  const [selected, setSelected] = useState(null)

  const load = () => {
    getUsers(search, activeOnly)
      .then(setUsers)
      .catch((e) => setError(e.message))
  }

  // Vuelve a buscar cuando cambia el texto o la casilla. Espera 300 ms a que se termine de escribir.
  useEffect(() => {
    const timer = setTimeout(load, 300)
    return () => clearTimeout(timer)
  }, [search, activeOnly])

  const openForm = (user) => {
    setSelected(user)
    setFormOpen(true)
  }

  const onSaved = (message) => {
    setFormOpen(false)
    setSuccess(message)
    load()
  }

  const toggleActive = async (user) => {
    try {
      if (user.active) {
        if (!window.confirm(`¿Dar de baja a "${user.username}"? No va a poder entrar al sistema.`)) return
        await deactivateUser(user.id)
        setSuccess(`"${user.username}" fue dado de baja.`)
      } else {
        await activateUser(user.id)
        setSuccess(`"${user.username}" fue reactivado.`)
      }
      load()
    } catch (e) {
      setError(e.message) // aca llega el rechazo si es el ultimo administrador
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Usuarios</h1>
          <p className="subtitle">Usuarios del sistema y sus roles.</p>
        </div>
        <button type="button" className="button button-primary" onClick={() => openForm(null)}>
          + Nuevo usuario
        </button>
      </div>

      <div className="filters">
        <input
          type="search"
          placeholder="Buscar por nombre, apellido o usuario..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          aria-label="Buscar usuarios"
        />
        <label className="checkbox">
          <input type="checkbox" checked={activeOnly} onChange={(event) => setActiveOnly(event.target.checked)} />
          Ver solo activos
        </label>
      </div>

      <Message type="error" text={error} onClose={() => setError('')} />
      <Message type="success" text={success} onClose={() => setSuccess('')} />

      {users.length === 0 ? (
        <p className="notice">No se encontraron usuarios.</p>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Nombre y apellido</th>
                <th>Rol</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id} className={user.active ? '' : 'inactive-row'}>
                  <td>{user.username}</td>
                  <td>{user.name} {user.lastName}</td>
                  <td>
                    <span className={user.role === 'ADMIN' ? 'chip chip-admin' : 'chip'}>
                      {user.role === 'ADMIN' ? 'Administrador' : 'Empleado'}
                    </span>
                  </td>
                  <td>
                    <span className={user.active ? 'chip chip-active' : 'chip chip-inactive'}>
                      {user.active ? 'Activo' : 'Dado de baja'}
                    </span>
                  </td>
                  <td className="actions">
                    <button type="button" className="button button-small" onClick={() => openForm(user)}>
                      Editar
                    </button>
                    <button type="button" className="button button-small button-secondary" onClick={() => toggleActive(user)}>
                      {user.active ? 'Dar de baja' : 'Reactivar'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {formOpen && <UserForm user={selected} onSaved={onSaved} onCancel={() => setFormOpen(false)} />}
    </>
  )
}
