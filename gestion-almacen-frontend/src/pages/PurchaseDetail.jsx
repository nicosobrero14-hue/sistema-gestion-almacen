import { Banknote, Receipt, Wallet } from 'lucide-react'
import { useState } from 'react'
import {
  addPayment, cancelPayment, deletePurchaseImage, purchaseImageUrl, uploadPurchaseImage,
} from '../api/purchases'
import Message from '../components/Message'
import Modal from '../components/Modal'
import TextField from '../components/TextField'
import { formatDate, formatPrice, PAYMENT_METHODS, PURCHASE_STATUS, voucherTypeName } from '../utils/format'

// La fecha de hoy, armada con la hora local. toISOString() la da en UTC, y despues de las
// nueve de la noche en Argentina eso ya es maniana: el servidor lo rechazaba por futura.
const hoy = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// Ficha de una factura de proveedor (RF-15 / CU-24).
// Acá se registran los pagos y se carga o reemplaza la foto del comprobante, que al dar de alta
// puede no haber estado a mano. onChanged avisa al listado que el saldo cambió.
export default function PurchaseDetail({ purchase: inicial, onChanged, onClose }) {
  const [purchase, setPurchase] = useState(inicial)
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  // El formulario del pago arranca cerrado: lo normal al abrir la ficha es solo mirarla.
  const [pagando, setPagando] = useState(false)
  const [pago, setPago] = useState({ date: hoy(), amount: '', method: 'EFECTIVO' })
  const [errors, setErrors] = useState({})

  const saldo = Number(purchase.balance)
  const corregida = Number(purchase.agreedTotal) !== Number(purchase.invoicedTotal)

  // Toda operación devuelve la factura al día, así que alcanza con reemplazarla.
  const actualizar = (actualizada) => {
    setPurchase(actualizada)
    onChanged()
  }

  const onPay = async (event) => {
    event.preventDefault()
    setGuardando(true)
    setErrors({})
    setError('')
    try {
      actualizar(await addPayment(purchase.id, {
        date: pago.date || null,
        amount: pago.amount === '' ? null : Number(pago.amount),
        method: pago.method,
      }))
      setPagando(false)
      setPago({ date: hoy(), amount: '', method: 'EFECTIVO' })
    } catch (e) {
      setErrors(e.errors)
      setError(e.message)
    } finally {
      setGuardando(false)
    }
  }

  const onCancelPayment = async (paymentId) => {
    if (!window.confirm('¿Anular este pago? El saldo vuelve atrás.')) return
    try {
      actualizar(await cancelPayment(purchase.id, paymentId))
    } catch (e) {
      setError(e.message)
    }
  }

  const onFileChange = async (event) => {
    const elegido = event.target.files[0]
    if (!elegido) return
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(elegido.type)) {
      setError('La foto tiene que ser JPG, PNG o WEBP.')
      return
    }
    setError('')
    try {
      actualizar(await uploadPurchaseImage(purchase.id, elegido))
    } catch (e) {
      setError(e.message)
    }
  }

  const onRemoveImage = async () => {
    if (!window.confirm('¿Quitar la foto del comprobante?')) return
    try {
      await deletePurchaseImage(purchase.id)
      actualizar({ ...purchase, image: null })
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <Modal title={`Factura ${purchase.voucherNumber}`} onClose={onClose}>
      <Message text={error} onClose={() => setError('')} />

      <p className="subtitle">
        {voucherTypeName(purchase.voucherType)} · {purchase.supplier?.name} · {formatDate(purchase.date)}
      </p>

      <div className="cards">
        <div className="card">
          <span className="card-icon"><Receipt aria-hidden="true" /></span>
          <div className="card-body">
            <span className="card-label">Total</span>
            <strong className="card-value">{formatPrice(purchase.agreedTotal)}</strong>
            {corregida && (
              <small className="card-note">
                Facturado {formatPrice(purchase.invoicedTotal)}. {purchase.adjustmentReason}
              </small>
            )}
          </div>
        </div>
        <div className="card">
          <span className="card-icon"><Banknote aria-hidden="true" /></span>
          <div className="card-body">
            <span className="card-label">Pagado</span>
            <strong className="card-value">{formatPrice(purchase.paid)}</strong>
          </div>
        </div>
        <div className={saldo > 0 ? 'card card-warning' : 'card'}>
          <span className="card-icon"><Wallet aria-hidden="true" /></span>
          <div className="card-body">
            <span className="card-label">{PURCHASE_STATUS[purchase.status]}</span>
            <strong className="card-value">{formatPrice(saldo)}</strong>
          </div>
        </div>
      </div>

      {/* La foto puede no haber estado a mano al cargar la factura, así que se puede sumar después. */}
      <div className="image-field">
        {purchase.image
          ? (
            <a href={purchaseImageUrl(purchase.image)} target="_blank" rel="noreferrer">
              <img className="voucher-preview" src={purchaseImageUrl(purchase.image)} alt="Comprobante" />
            </a>
          )
          : <span className="voucher-preview voucher-preview-empty" aria-hidden="true">📄</span>}
        <div>
          <label htmlFor="image">Foto del comprobante</label>
          <input id="image" type="file" accept="image/jpeg,image/png,image/webp" onChange={onFileChange} />
          <small className="help">
            {purchase.image
              ? 'Hacé clic en la foto para verla completa y revisar el detalle de lo que vino.'
              : 'Todavía no tiene. Se puede sacar ahora y cargarla acá.'}
          </small>
          {purchase.image && (
            <button type="button" className="button button-small button-secondary" onClick={onRemoveImage}>
              Quitar foto
            </button>
          )}
        </div>
      </div>

      <h3>Pagos</h3>

      {purchase.payments.length === 0 ? (
        <p className="notice">Todavía no se le pagó nada.</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Forma</th>
              <th className="right">Monto</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {purchase.payments.map((payment) => (
              <tr key={payment.id} className={payment.cancelled ? 'inactive-row' : ''}>
                <td className="nowrap">{formatDate(payment.date)}</td>
                <td>
                  {PAYMENT_METHODS[payment.method]}
                  {payment.cancelled && <> · <strong>anulado</strong></>}
                </td>
                <td className="right">{formatPrice(payment.amount)}</td>
                <td className="actions">
                  {!payment.cancelled && (
                    <button type="button" className="button button-small button-secondary"
                      onClick={() => onCancelPayment(payment.id)}>
                      Anular
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {saldo > 0 && !pagando && (
        <div className="form-actions">
          <button type="button" className="button button-primary" onClick={() => setPagando(true)}>
            Registrar pago
          </button>
        </div>
      )}

      {pagando && (
        <form onSubmit={onPay} noValidate>
          <div className="fields-row">
            <TextField name="date" label="Fecha del pago" type="date" value={pago.date}
              onChange={(e) => setPago({ ...pago, date: e.target.value })} error={errors.date} required />
            <TextField name="amount" label="Monto" type="number" step="0.01" min="0" value={pago.amount}
              onChange={(e) => setPago({ ...pago, amount: e.target.value })} error={errors.amount} required
              help={`Saldo: ${formatPrice(saldo)}`} />
            <div className="field">
              <label htmlFor="method">Forma de pago</label>
              <select id="method" value={pago.method}
                onChange={(e) => setPago({ ...pago, method: e.target.value })}>
                {Object.keys(PAYMENT_METHODS).map((m) => (
                  <option key={m} value={m}>{PAYMENT_METHODS[m]}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="form-actions">
            <button type="button" className="button button-secondary" onClick={() => setPagando(false)}>
              Cancelar
            </button>
            <button type="submit" className="button button-primary" disabled={guardando}>
              {guardando ? 'Guardando...' : 'Registrar pago'}
            </button>
          </div>
        </form>
      )}
    </Modal>
  )
}
