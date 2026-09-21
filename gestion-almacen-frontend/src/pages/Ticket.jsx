import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { findSale } from '../api/sales'
import Message from '../components/Message'
import { formatDateTime, formatPrice, formatTicketNumber, PAYMENT_METHODS } from '../utils/format'

// Ticket de una venta (RF-06 / CU-14). Se arma con los datos guardados de la venta, asi se puede volver a abrir.
// Imprimir usa la impresora que elija el usuario, por ejemplo la de tickets (CU-15).
export default function Ticket() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()

  const [sale, setSale] = useState(null)
  const [error, setError] = useState('')
  // Si se llega desde la venta recien confirmada, se muestra el aviso.
  const [success, setSuccess] = useState(location.state?.created ? 'La venta quedó registrada.' : '')

  useEffect(() => {
    findSale(id)
      .then(setSale)
      .catch((e) => setError(e.message))
  }, [id])

  return (
    <>
      {/* no-print: el encabezado y los botones no salen en el papel. */}
      <div className="page-header no-print">
        <div>
          <h1>Ticket</h1>
          <p className="subtitle">Comprobante de la venta.</p>
        </div>
        <div className="header-actions">
          <button type="button" className="button" onClick={() => window.print()} disabled={!sale}>
            Imprimir
          </button>
          {/* Desde el historial se vuelve atras, asi los filtros siguen puestos. */}
          {location.state?.fromHistory ? (
            <button type="button" className="button button-primary" onClick={() => navigate(-1)}>
              Volver al historial
            </button>
          ) : (
            <Link to="/sale" className="button button-primary">Nueva venta</Link>
          )}
        </div>
      </div>

      <Message type="error" text={error} onClose={() => setError('')} />
      <Message type="success" text={success} onClose={() => setSuccess('')} />

      {sale && (
        <div className="ticket">
          <div className="ticket-header">
            <strong>Comprobante de venta</strong>
            <span>Ticket N° {formatTicketNumber(sale.id)}</span>
            <span>{formatDateTime(sale.dateTime)}</span>
            <span>Atendió: {sale.username}</span>
          </div>

          <div className="ticket-lines">
            {sale.details.map((detail) => (
              <div key={detail.id} className="ticket-line">
                <span>{detail.quantity} x {detail.productName}</span>
                <span className="ticket-amounts">
                  <small>{formatPrice(detail.unitPrice)} c/u</small>
                  {formatPrice(detail.subtotal)}
                </span>
              </div>
            ))}
          </div>

          <div className="ticket-totals">
            <div className="ticket-line">
              <span>Subtotal</span>
              <span>{formatPrice(sale.subtotal)}</span>
            </div>
            {Number(sale.discount) > 0 && (
              <div className="ticket-line">
                <span>Descuento</span>
                <span>-{formatPrice(sale.discount)}</span>
              </div>
            )}
            <div className="ticket-line ticket-total">
              <span>TOTAL</span>
              <span>{formatPrice(sale.total)}</span>
            </div>
            {sale.payments.map((payment) => (
              <div key={payment.id} className="ticket-line">
                <span>Pago: {PAYMENT_METHODS[payment.method]}</span>
                <span>{formatPrice(payment.amount)}</span>
              </div>
            ))}
          </div>

          <div className="ticket-footer">
            <span>¡Gracias por su compra!</span>
            <small>Comprobante no válido como factura</small>
          </div>
        </div>
      )}
    </>
  )
}
