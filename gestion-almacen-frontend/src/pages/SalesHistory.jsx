import { ArrowLeftRight, Banknote, CreditCard, Receipt, Wallet } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getSales, getSalesSummary } from '../api/sales'
import { getUsers } from '../api/users'
import Message from '../components/Message'
import Pagination from '../components/Pagination'
import { formatDateTime, formatPrice, formatTicketNumber, PAYMENT_METHODS, SALE_STATUS } from '../utils/format'

// Cuantas ventas entran en una pagina.
const POR_PAGINA = 20

// Un icono por forma de pago, el mismo que usa el panel principal.
const METHOD_ICONS = {
  EFECTIVO: Banknote,
  TRANSFERENCIA: ArrowLeftRight,
  MERCADOPAGO: CreditCard,
}

// Fecha en el formato de los campos de fecha y de la API: 2026-09-21
const toIsoDate = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

// Si no se eligen fechas, se muestra el mes en curso: del dia 1 a hoy.
const today = new Date()
const DEFAULT_FROM = toIsoDate(new Date(today.getFullYear(), today.getMonth(), 1))
const DEFAULT_TO = toIsoDate(today)

// Los dos primeros productos de la venta y cuantos mas hay.
const productsText = (sale) => {
  const names = sale.details.map((detail) => detail.productName)
  const rest = names.length - 2
  return names.slice(0, 2).join(', ') + (rest > 0 ? ` y ${rest} más` : '')
}

// Historial de ventas (RF-07 / CU-16). Solo el administrador.
// Los filtros viven en la direccion (/sales?from=...&to=...): al volver de un ticket siguen puestos.
export default function SalesHistory() {
  const [searchParams, setSearchParams] = useSearchParams()
  const from = searchParams.get('from') ?? DEFAULT_FROM
  const to = searchParams.get('to') ?? DEFAULT_TO
  const username = searchParams.get('username') ?? ''
  const product = searchParams.get('product') ?? ''
  const reference = searchParams.get('reference') ?? ''

  const [sales, setSales] = useState([])
  const [users, setUsers] = useState([])
  const [error, setError] = useState('')

  // Pagina del listado (RF-13).
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [total, setTotal] = useState(0)

  // CU-16 paso 6: el resumen lo calcula el servidor sobre todas las ventas del filtro.
  const [summary, setSummary] = useState({ charged: 0, total: 0, byMethod: {} })

  // Para el filtro de empleado: todos los usuarios, incluidos los dados de baja, que pueden tener ventas.
  useEffect(() => {
    getUsers('', false)
      .then(setUsers)
      .catch((e) => setError(e.message))
  }, [])

  // CU-16 paso 4: vuelve a buscar cuando cambia un filtro. Espera 300 ms a que se termine de escribir.
  useEffect(() => {
    const timer = setTimeout(() => {
      const filtros = { from, to, username, product, reference }
      getSales({ ...filtros, page, size: POR_PAGINA })
        .then((data) => {
          setSales(data.content)
          setTotalPages(data.totalPages)
          setTotal(data.totalElements)
          setError('')
        })
        .catch((e) => {
          setSales([])
          setError(e.message)
        })
      getSalesSummary(filtros)
        .then(setSummary)
        .catch(() => setSummary({ charged: 0, total: 0, byMethod: {} }))
    }, 300)
    return () => clearTimeout(timer)
  }, [from, to, username, product, reference, page])

  // Un filtro nuevo empieza en la primera pagina.
  useEffect(() => {
    setPage(0)
  }, [from, to, username, product, reference])

  // Guarda el filtro en la direccion. Un campo vacio vuelve al valor por defecto.
  const changeFilter = (event) => {
    const params = new URLSearchParams(searchParams)
    if (event.target.value) {
      params.set(event.target.name, event.target.value)
    } else {
      params.delete(event.target.name)
    }
    setSearchParams(params, { replace: true })
  }

  return (
    <>
      <h1>Historial de ventas</h1>
      <p className="subtitle">Ventas registradas, de la más nueva a la más vieja.</p>

      <div className="filters">
        <label className="filter">
          Desde
          <input type="date" name="from" value={from} max={to} onChange={changeFilter} />
        </label>
        <label className="filter">
          Hasta
          <input type="date" name="to" value={to} min={from} onChange={changeFilter} />
        </label>
        <label className="filter">
          Empleado
          <select name="username" value={username} onChange={changeFilter}>
            <option value="">Todos</option>
            {users.map((user) => (
              <option key={user.id} value={user.username}>
                {user.name} {user.lastName} ({user.username})
              </option>
            ))}
          </select>
        </label>
        <input
          type="search"
          name="product"
          placeholder="Producto..."
          value={product}
          onChange={changeFilter}
          aria-label="Filtrar por producto"
        />
        {/* CU-10: para encontrar la venta de un cobro que figura en el reporte de MercadoPago. */}
        <input
          type="search"
          name="reference"
          placeholder="N° de operación..."
          value={reference}
          onChange={changeFilter}
          aria-label="Filtrar por número de operación"
        />
      </div>

      <Message type="error" text={error} onClose={() => setError('')} />

      <div className="cards">
        <div className="card">
          <span className="card-icon"><Receipt aria-hidden="true" /></span>
          <div className="card-body">
            <span className="card-label">Ventas cobradas</span>
            <strong className="card-value">{summary.charged}</strong>
          </div>
        </div>
        <div className="card card-main">
          <span className="card-icon"><Wallet aria-hidden="true" /></span>
          <div className="card-body">
            <span className="card-label">Total recaudado</span>
            <strong className="card-value">{formatPrice(summary.total)}</strong>
          </div>
        </div>
        {Object.keys(PAYMENT_METHODS).map((method) => {
          const Icon = METHOD_ICONS[method] ?? Banknote
          return (
            <div key={method} className="card">
              <span className="card-icon"><Icon aria-hidden="true" /></span>
              <div className="card-body">
                <span className="card-label">{PAYMENT_METHODS[method]}</span>
                <strong className="card-value">{formatPrice(summary.byMethod[method] ?? 0)}</strong>
              </div>
            </div>
          )
        })}
      </div>

      {/* CU-16 exc. 3a: no hay ventas con esos filtros. */}
      {sales.length === 0 ? (
        <p className="notice">No se encontraron ventas con esos filtros.</p>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>N°</th>
                <th>Fecha</th>
                <th>Empleado</th>
                <th>Productos</th>
                <th>Forma de pago</th>
                <th className="right">Total</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {sales.map((sale) => (
                <tr key={sale.id}>
                  <td>
                    {formatTicketNumber(sale.id)}
                    {/* Lo normal es que este cobrada: solo se marca la que no lo esta. */}
                    {sale.status !== 'CONFIRMADA' && (
                      <div><span className="chip chip-inactive">{SALE_STATUS[sale.status]}</span></div>
                    )}
                  </td>
                  <td className="nowrap">{formatDateTime(sale.dateTime)}</td>
                  <td>{sale.employeeName || sale.username}</td>
                  <td>{productsText(sale)}</td>
                  <td>
                    {sale.payments.map((payment) => PAYMENT_METHODS[payment.method]).join(', ')}
                    {sale.payments.filter((payment) => payment.externalReference).map((payment) => (
                      <div key={payment.id}><small className="help">N° {payment.externalReference}</small></div>
                    ))}
                  </td>
                  <td className="right">{formatPrice(sale.total)}</td>
                  {/* CU-16 paso 5: el detalle de la venta es su ticket. */}
                  <td className="actions">
                    <Link to={`/sales/${sale.id}`} state={{ fromHistory: true }} className="button button-small">
                      Ver ticket
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <Pagination page={page} totalPages={totalPages} totalElements={total}
            name="venta" onChange={setPage} />
        </div>
      )}
    </>
  )
}
