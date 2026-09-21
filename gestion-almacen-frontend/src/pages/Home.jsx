import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client'
import { getExpiringProducts, getLowStockProducts } from '../api/stock'

// Se avisa de los productos que vencen dentro de este plazo.
const EXPIRATION_DAYS = 30

// "2026-10-15" -> "15/10/2026"
const formatDate = (value) => value.split('-').reverse().join('/')

// Dias que faltan hasta la fecha. Negativo si ya paso.
const daysUntil = (value) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((new Date(`${value}T00:00:00`) - today) / 86400000)
}

const expirationText = (days) => {
  if (days < 0) return `Venció hace ${-days} ${days === -1 ? 'día' : 'días'}`
  if (days === 0) return 'Vence hoy'
  return `Vence en ${days} ${days === 1 ? 'día' : 'días'}`
}

// Panel principal: estado del sistema y alertas de stock bajo y vencimiento (RF-09 / CU-06).
export default function Home({ user }) {
  const [status, setStatus] = useState(null)
  const [lowStock, setLowStock] = useState([])
  const [expiring, setExpiring] = useState([])
  const [error, setError] = useState('')

  // CU-06 pasos 2 y 3: las alertas se calculan cada vez que se abre el panel.
  useEffect(() => {
    client.get('/status')
      .then((response) => setStatus(response.data))
      .catch(() => setError('No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.'))
    getLowStockProducts()
      .then(setLowStock)
      .catch((e) => setError(e.message))
    getExpiringProducts(EXPIRATION_DAYS)
      .then(setExpiring)
      .catch((e) => setError(e.message))
  }, [])

  return (
    <>
      <h1>Panel principal</h1>
      <p className="subtitle">Bienvenido, {user.name}. Este es el estado del sistema.</p>

      {error && <p className="alert">{error}</p>}

      {status && (
        <div className="cards">
          <div className="card">
            <span className="card-label">Aplicación</span>
            <strong className="card-value">{status.application}</strong>
          </div>
          <div className="card">
            <span className="card-label">Base de datos</span>
            <strong className="card-value">{status.database}</strong>
          </div>
          <div className={lowStock.length > 0 ? 'card card-warning' : 'card'}>
            <span className="card-label">Stock bajo</span>
            <strong className="card-value">{lowStock.length}</strong>
          </div>
          <div className={expiring.length > 0 ? 'card card-warning' : 'card'}>
            <span className="card-label">Por vencer</span>
            <strong className="card-value">{expiring.length}</strong>
          </div>
        </div>
      )}

      <div className="panels">
        <section className="panel">
          <h2>Stock bajo</h2>
          {/* CU-06 exc. 2a: sin alertas, se informa con un mensaje. */}
          {lowStock.length === 0 ? (
            <p className="notice">No hay productos con stock bajo.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th className="right">Stock</th>
                  <th className="right">Mínimo</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {lowStock.map((product) => (
                  <tr key={product.id}>
                    <td>{product.name}</td>
                    <td className="right low-stock">{product.stock}</td>
                    <td className="right">{product.minimumStock}</td>
                    {/* CU-06 paso 6: acceso directo al producto para ajustarlo. */}
                    <td className="actions">
                      <Link to={`/stock?search=${encodeURIComponent(product.name)}`} className="button button-small">
                        Ajustar stock
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section className="panel">
          <h2>Próximos a vencer ({EXPIRATION_DAYS} días)</h2>
          {expiring.length === 0 ? (
            <p className="notice">No hay productos que venzan en los próximos {EXPIRATION_DAYS} días.</p>
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
                  const days = daysUntil(product.expirationDate)
                  return (
                    <tr key={product.id}>
                      <td>{product.name}</td>
                      <td>
                        {formatDate(product.expirationDate)}
                        <small className={days < 0 ? 'expiration expired' : 'expiration'}>{expirationText(days)}</small>
                      </td>
                      <td className="right">{product.stock}</td>
                      <td className="actions">
                        <Link to={`/products?search=${encodeURIComponent(product.name)}`} className="button button-small">
                          Ver producto
                        </Link>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </>
  )
}
