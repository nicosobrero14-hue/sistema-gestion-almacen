import { useState } from 'react'
import { putOnOffer } from '../api/offers'
import Message from '../components/Message'
import Modal from '../components/Modal'
import TextField from '../components/TextField'
import { formatPrice } from '../utils/format'

// Poner un producto en oferta con su precio especial (CU-19 paso 4).
export default function OfferForm({ product, onSaved, onCancel }) {
  const [offerPrice, setOfferPrice] = useState('')
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  // CU-19 exc. 4a: se avisa mientras se escribe si la oferta no es mas barata que el precio normal.
  const value = offerPrice === '' ? null : Number(offerPrice)
  const priceError = value !== null && value >= Number(product.price) ? 'Tiene que ser menor al precio normal' : null
  const discount = value && !priceError ? Math.round((1 - value / product.price) * 100) : null

  const onSubmit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    setError('')
    try {
      onSaved(await putOnOffer(product.id, { offerPrice: value }))
    } catch (e) {
      setErrors(e.errors)
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={`Poner en oferta: ${product.name}`} onClose={onCancel}>
      <p className="stock-summary">
        Precio normal: <strong>{formatPrice(product.price)}</strong>
        <span>Stock: <strong>{product.stock}</strong></span>
      </p>

      <Message text={error} onClose={() => setError('')} />

      <form onSubmit={onSubmit} noValidate>
        <TextField name="offerPrice" label="Precio de oferta" type="number" min="0" step="0.01" value={offerPrice}
          onChange={(event) => setOfferPrice(event.target.value)} error={errors.offerPrice ?? priceError} required autoFocus
          help={discount ? `${discount}% menos que el precio normal` : 'Tiene que ser menor al precio normal'} />

        <div className="form-actions">
          <button type="button" className="button button-secondary" onClick={onCancel}>Cancelar</button>
          <button type="submit" className="button button-primary" disabled={saving || Boolean(priceError)}>
            {saving ? 'Guardando...' : 'Guardar oferta'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
