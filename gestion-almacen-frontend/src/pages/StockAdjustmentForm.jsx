import { useState } from 'react'
import { adjustStock } from '../api/stock'
import Message from '../components/Message'
import Modal from '../components/Modal'
import TextField from '../components/TextField'

// Un campo vacio viaja como null, asi el backend avisa que es obligatorio en vez de recibir un cero.
const toNumber = (value) => (value === '' ? null : Number(value))

// Ajuste manual de stock (CU-04). Se ingresa el stock que hay ahora, no la diferencia, y el motivo.
export default function StockAdjustmentForm({ product, onSaved, onCancel }) {
  const [data, setData] = useState({ newStock: '', reason: '' })
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const onChange = (event) => setData({ ...data, [event.target.name]: event.target.value })

  // Muestra cuanto sube o baja el stock mientras se escribe, para evitar errores de tipeo.
  const difference = data.newStock === '' ? null : Number(data.newStock) - product.stock
  const differenceText = difference === null ? null : `Diferencia: ${difference > 0 ? '+' : ''}${difference}`

  const onSubmit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    setError('')

    try {
      const updated = await adjustStock(product.id, { newStock: toNumber(data.newStock), reason: data.reason })
      onSaved(updated)
    } catch (e) {
      setErrors(e.errors)
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={`Ajustar stock: ${product.name}`} onClose={onCancel}>
      <p className="stock-summary">
        Stock actual: <strong>{product.stock}</strong>
        <span>Mínimo: <strong>{product.minimumStock}</strong></span>
      </p>

      <Message text={error} onClose={() => setError('')} />

      <form onSubmit={onSubmit} noValidate>
        <TextField name="newStock" label="Stock nuevo" type="number" min="0" value={data.newStock}
          onChange={onChange} error={errors.newStock} required autoFocus
          help={differenceText ?? 'La cantidad que hay ahora en el comercio'} />

        <TextField name="reason" label="Motivo" value={data.reason} onChange={onChange}
          error={errors.reason} required maxLength={255}
          help="Por ejemplo: ingreso de mercadería, rotura, vencimiento, corrección de conteo" />

        <div className="form-actions">
          <button type="button" className="button button-secondary" onClick={onCancel}>Cancelar</button>
          <button type="submit" className="button button-primary" disabled={saving}>
            {saving ? 'Guardando...' : 'Guardar ajuste'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
