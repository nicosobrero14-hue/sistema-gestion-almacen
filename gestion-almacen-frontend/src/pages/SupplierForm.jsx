import { useState } from 'react'
import { editSupplier, saveSupplier } from '../api/suppliers'
import Message from '../components/Message'
import Modal from '../components/Modal'
import TextField from '../components/TextField'

const EMPTY = { name: '', lastName: '', email: '', phone: '', address: '' }

// Alta y modificacion de proveedores (CU-05). Si supplier es null, es un alta.
export default function SupplierForm({ supplier, onSaved, onCancel }) {
  const [data, setData] = useState(supplier ?? EMPTY)
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

    // Los campos vacios viajan como null: el email es unico y la base no acepta dos emails vacios.
    const body = {
      name: data.name,
      lastName: data.lastName || null,
      email: data.email || null,
      phone: data.phone || null,
      address: data.address || null,
    }

    try {
      if (supplier) {
        await editSupplier(supplier.id, body)
        onSaved(`"${body.name}" fue modificado.`)
      } else {
        await saveSupplier(body)
        onSaved(`"${body.name}" fue creado.`)
      }
    } catch (e) {
      setErrors(e.errors)
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={supplier ? `Editar proveedor: ${supplier.name}` : 'Nuevo proveedor'} onClose={onCancel}>
      <Message text={error} onClose={() => setError('')} />

      {/* noValidate: los mensajes los da el backend, en español e iguales en todos los navegadores. */}
      <form onSubmit={onSubmit} noValidate>
        <div className="fields-row">
          <TextField name="name" label="Nombre o razón social" value={data.name} onChange={onChange}
            error={errors.name} required maxLength={80} autoFocus />
          <TextField name="lastName" label="Apellido" value={data.lastName} onChange={onChange}
            error={errors.lastName} maxLength={80} help="Solo si es una persona" />
        </div>

        <div className="fields-row">
          <TextField name="email" label="Email" type="email" value={data.email} onChange={onChange}
            error={errors.email} maxLength={120} />
          <TextField name="phone" label="Teléfono" value={data.phone} onChange={onChange}
            error={errors.phone} maxLength={30} />
        </div>

        <TextField name="address" label="Dirección" value={data.address} onChange={onChange}
          error={errors.address} maxLength={150} />

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
