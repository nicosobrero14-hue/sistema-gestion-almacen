import {
  ArrowLeftRight, Banknote, Boxes, CalendarClock, CreditCard, Receipt, ShoppingBag, TrendingUp,
  Wallet,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getStatistics } from '../api/statistics'
import { getExpiringProducts, getLowStockProducts } from '../api/stock'
import SalesChart from '../components/SalesChart'
import {
  daysUntil, expirationText, formatDate, formatPrice, formatQuantity, PAYMENT_METHODS,
} from '../utils/format'

// Se avisa de los productos que vencen dentro de este plazo.
const EXPIRATION_DAYS = 30

// Un icono por forma de pago, para distinguir las tarjetas de un vistazo.
const METHOD_ICONS = {
  EFECTIVO: Banknote,
  TRANSFERENCIA: ArrowLeftRight,
  MERCADOPAGO: CreditCard,
}

// Panel principal (RF-09, RF-15 / CU-06): como viene el dia y las alertas de stock y vencimiento.
//
// Lo que ve cada uno depende del rol, y la diferencia la hace el servidor: al empleado no se le
// envian la ganancia ni la deuda a proveedores. El empleado si ve lo cobrado y como se reparte,
// que es lo que necesita para cerrar su caja al terminar el turno.
export default function Home({ user }) {
  const isAdmin = user.role === 'ADMIN'

  const [stats, setStats] = useState(null)
  const [lowStock, setLowStock] = useState([])
  const [expiring, setExpiring] = useState([])
  const [error, setError] = useState('')

  // CU-06 pasos 2 y 3: los numeros y las alertas se calculan cada vez que se abre el panel.
  useEffect(() => {
    getStatistics()
      .then(setStats)
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
      <p className="subtitle">
        Bienvenido, {user.name}. {isAdmin ? 'Así viene el día.' : 'Así viene la caja de hoy.'}
      </p>

      {error && <p className="alert">{error}</p>}

      {stats && (
        <>
          <div className="cards">
            <div className="card card-main">
              <span className="card-icon"><Wallet aria-hidden="true" /></span>
              <div className="card-body">
                <span className="card-label">Vendido hoy</span>
                <strong className="card-value">{formatPrice(stats.soldToday)}</strong>
                <small className="card-note">
                  {stats.ticketsToday === 1 ? '1 venta' : `${stats.ticketsToday} ventas`}
                  {stats.averageTicket && ` · ${formatPrice(stats.averageTicket)} promedio`}
                </small>
              </div>
            </div>

            {/* La ganancia es del comercio: el servidor no la manda si quien mira es empleado. */}
            {stats.profitToday !== undefined && (
              <div className="card card-main">
                <span className="card-icon"><TrendingUp aria-hidden="true" /></span>
                <div className="card-body">
                  <span className="card-label">Ganancia de hoy</span>
                  <strong className="card-value">{formatPrice(stats.profitToday)}</strong>
                  <small className="card-note">
                    {stats.linesWithoutCost > 0
                      ? `${stats.linesWithoutCost} ${stats.linesWithoutCost === 1 ? 'renglón' : 'renglones'} sin precio de costo cargado`
                      : 'Lo vendido menos lo que costó esa mercadería'}
                  </small>
                </div>
              </div>
            )}

            {Object.keys(PAYMENT_METHODS).map((method) => {
              const Icon = METHOD_ICONS[method] ?? Banknote
              return (
                <div key={method} className="card">
                  <span className="card-icon"><Icon aria-hidden="true" /></span>
                  <div className="card-body">
                    <span className="card-label">{PAYMENT_METHODS[method]}</span>
                    <strong className="card-value">{formatPrice(stats.byMethod[method] ?? 0)}</strong>
                  </div>
                </div>
              )
            })}

            {stats.monthPurchases !== undefined && (
              <div className="card">
                <span className="card-icon"><ShoppingBag aria-hidden="true" /></span>
                <div className="card-body">
                  <span className="card-label">Compras del mes</span>
                  <strong className="card-value">{formatPrice(stats.monthPurchases)}</strong>
                </div>
              </div>
            )}

            {stats.supplierDebt !== undefined && (
              <div className={Number(stats.supplierDebt) > 0 ? 'card card-warning' : 'card'}>
                <span className="card-icon"><Receipt aria-hidden="true" /></span>
                <div className="card-body">
                  <span className="card-label">Deuda a proveedores</span>
                  <strong className="card-value">{formatPrice(stats.supplierDebt)}</strong>
                </div>
              </div>
            )}

            <div className={lowStock.length > 0 ? 'card card-warning' : 'card'}>
              <span className="card-icon"><Boxes aria-hidden="true" /></span>
              <div className="card-body">
                <span className="card-label">Stock bajo</span>
                <strong className="card-value">{lowStock.length}</strong>
              </div>
            </div>

            <div className={expiring.length > 0 ? 'card card-warning' : 'card'}>
              <span className="card-icon"><CalendarClock aria-hidden="true" /></span>
              <div className="card-body">
                <span className="card-label">Por vencer</span>
                <strong className="card-value">{expiring.length}</strong>
              </div>
            </div>
          </div>

          {/* El grafico y lo mas vendido son del administrador, como la ganancia. */}
          {stats.lastDays && (
            <div className="panels panels-wide">
              <section className="panel">
                <h2>Ventas de las últimas dos semanas</h2>
                <SalesChart days={stats.lastDays} />
              </section>

              <section className="panel">
                <h2>Lo más vendido del mes</h2>
                {stats.topProducts.length === 0 ? (
                  <p className="notice">Todavía no hay ventas este mes.</p>
                ) : (
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Producto</th>
                        <th className="right">Cantidad</th>
                        <th className="right">Importe</th>
                      </tr>
                    </thead>
                    <tbody>
                      {stats.topProducts.map((product) => (
                        <tr key={product.name}>
                          <td>{product.name}</td>
                          <td className="right">{Number(product.quantity)}</td>
                          <td className="right">{formatPrice(product.total)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </section>
            </div>
          )}
        </>
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
                    <td className="right low-stock">{formatQuantity(product.stock, product.saleUnit)}</td>
                    <td className="right">{formatQuantity(product.minimumStock, product.saleUnit)}</td>
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
                      <td className="right">{formatQuantity(product.stock, product.saleUnit)}</td>
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
