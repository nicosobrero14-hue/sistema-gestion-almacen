import { useEffect, useState } from 'react'
import { endOffer, getLeastSoldProducts, getOffers } from '../api/offers'
import { getExpiringProducts } from '../api/stock'
import Message from '../components/Message'
import { daysUntil, expirationText, formatDate, formatPrice } from '../utils/format'
import OfferForm from './OfferForm'

// Plazos para las sugerencias (RF-10: configurable, por ejemplo 15 o 30 dias).
const PERIODS = [15, 30, 60]

// Cuanto mas barata es la oferta: 3400 y 2990 -> 12
const discountPercent = (product) => Math.round((1 - product.offerPrice / product.price) * 100)

// Ofertas (RF-10 / CU-19). Solo el administrador.
// El sistema sugiere candidatos pero nunca pone una oferta solo: la decision es del administrador.
export default function Offers() {
  const [days, setDays] = useState(30)
  const [offers, setOffers] = useState([])
  const [leastSold, setLeastSold] = useState([])
  const [expiring, setExpiring] = useState([])
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // El producto que se esta poniendo en oferta. null = ventana cerrada.
  const [selected, setSelected] = useState(null)

  // CU-19 paso 2: las sugerencias se calculan cada vez, con el plazo elegido.
  const load = () => {
    getOffers().then(setOffers).catch((e) => setError(e.message))
    getLeastSoldProducts(days).then(setLeastSold).catch((e) => setError(e.message))
    getExpiringProducts(days).then(setExpiring).catch((e) => setError(e.message))
  }

  useEffect(() => {
    load()
  }, [days])

  const onSaved = (product) => {
    setSelected(null)
    setSuccess(`"${product.name}" quedó en oferta a ${formatPrice(product.offerPrice)}.`)
    load()
  }

  // CU-19 paso 6: la oferta se quita en cualquier momento.
  const removeOffer = async (product) => {
    try {
      await endOffer(product.id)
      setSuccess(`"${product.name}" volvió a su precio normal.`)
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  // Lo que se muestra en la ultima columna de una sugerencia: el boton, o la oferta si ya tiene una.
  const offerAction = (product) => (product.onOffer ? (
    <span className="chip chip-admin">En oferta: {formatPrice(product.offerPrice)}</span>
  ) : (
    <button type="button" className="button button-small" onClick={() => setSelected(product)}>
      Poner en oferta
    </button>
  ))

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Ofertas</h1>
          <p className="subtitle">Productos sugeridos para promocionar. El sistema no aplica nada solo: la decisión es suya.</p>
        </div>
        <label className="filter">
          Plazo
          <select value={days} onChange={(event) => setDays(Number(event.target.value))}>
            {PERIODS.map((period) => (
              <option key={period} value={period}>{period} días</option>
            ))}
          </select>
        </label>
      </div>

      <Message type="error" text={error} onClose={() => setError('')} />
      <Message type="success" text={success} onClose={() => setSuccess('')} />

      <section className="panel panel-wide">
        <h2>Ofertas vigentes</h2>
        {offers.length === 0 ? (
          <p className="notice">No hay productos en oferta.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Producto</th>
                <th className="right">Precio normal</th>
                <th className="right">Precio de oferta</th>
                <th className="right">Descuento</th>
                <th className="right">Stock</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {offers.map((product) => (
                <tr key={product.id}>
                  <td>{product.name}</td>
                  <td className="right">{formatPrice(product.price)}</td>
                  <td className="right"><strong>{formatPrice(product.offerPrice)}</strong></td>
                  <td className="right">{discountPercent(product)}%</td>
                  <td className="right">{product.stock}</td>
                  <td className="actions">
                    <button type="button" className="button button-small button-secondary" onClick={() => removeOffer(product)}>
                      Quitar oferta
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <div className="panels">
        {/* RF-10: baja rotacion. */}
        <section className="panel">
          <h2>Menos vendidos en los últimos {days} días</h2>
          {leastSold.length === 0 ? (
            <p className="notice">No hay productos con stock para sugerir.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th className="right">Vendidos</th>
                  <th className="right">Stock</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {leastSold.map(({ product, unitsSold }) => (
                  <tr key={product.id}>
                    <td>{product.name}</td>
                    <td className="right">{unitsSold}</td>
                    <td className="right">{product.stock}</td>
                    <td className="actions">{offerAction(product)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        {/* RF-10: proximos a vencer. Es la misma consulta que la alerta del panel. */}
        <section className="panel">
          <h2>Vencen en los próximos {days} días</h2>
          {expiring.length === 0 ? (
            <p className="notice">No hay productos que venzan en ese plazo.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th>Vencimiento</th>
                  <th className="right">Stock</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {expiring.map((product) => {
                  const daysLeft = daysUntil(product.expirationDate)
                  return (
                    <tr key={product.id}>
                      <td>{product.name}</td>
                      <td>
                        {formatDate(product.expirationDate)}
                        <small className={daysLeft < 0 ? 'expiration expired' : 'expiration'}>{expirationText(daysLeft)}</small>
                      </td>
                      <td className="right">{product.stock}</td>
                      <td className="actions">{offerAction(product)}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
        </section>
      </div>

      {selected && <OfferForm product={selected} onSaved={onSaved} onCancel={() => setSelected(null)} />}
    </>
  )
}
