import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getSales } from '../api/sales'
import { getUsers } from '../api/users'
import Message from '../components/Message'
import { formatDateTime, formatPrice, formatTicketNumber, PAYMENT_METHODS } from '../utils/format'

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

  const [sales, setSales] = useState([])
  const [users, setUsers] = useState([])
  const [error, setError] = useState('')

  // Para el filtro de empleado: todos los usuarios, incluidos los dados de baja, que pueden tener ventas.
  useEffect(() => {
    getUsers('', false)
      .then(setUsers)
      .catch((e) => setError(e.message))
  }, [])

  // CU-16 paso 4: vuelve a buscar cuando cambia un filtro. Espera 300 ms a que se termine de escribir.
  useEffect(() => {
    const timer = setTimeout(() => {
      getSales({ from, to, username, product })
        .then((data) => {
          setSales(data)
          setError('')
        })
        .catch((e) => {
          setSales([])
          setError(e.message)
        })
    }, 300)
    return () => clearTimeout(timer)
  }, [from, to, username, product])

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

  // CU-16 paso 6: total recaudado en el periodo, y cuanto por cada forma de pago.
  const total = sales.reduce((sum, sale) => sum + Number(sale.total), 0)
  const totalByMethod = (method) => sales
    .flatMap((sale) => sale.payments)
    .filter((payment) => payment.method === method)
    .reduce((sum, payment) => sum + Number(payment.amount), 0)

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
      </div>

      <Message type="error" text={error} onClose={() => setError('')} />

      <div className="cards">
        <div className="card">
          <span className="card-label">Ventas</span>
          <strong className="card-value">{sales.length}</strong>
        </div>
        <div className="card">
          <span className="card-label">Total recaudado</span>
          <strong className="card-value">{formatPrice(total)}</strong>
        </div>
        {Object.keys(PAYMENT_METHODS).map((method) => (
          <div key={method} className="card">
            <span className="card-label">{PAYMENT_METHODS[method]}</span>
            <strong className="card-value">{formatPrice(totalByMethod(method))}</strong>
          </div>
        ))}
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
                  <td>{formatTicketNumber(sale.id)}</td>
                  <td className="nowrap">{formatDateTime(sale.dateTime)}</td>
                  <td>{sale.username}</td>
                  <td>{productsText(sale)}</td>
                  <td>{sale.payments.map((payment) => PAYMENT_METHODS[payment.method]).join(', ')}</td>
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
        </div>
      )}
    </>
  )
}
