import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getPurchases } from '../api/purchases'
import { getSuppliers } from '../api/suppliers'
import Message from '../components/Message'
import { formatDate, formatPrice, PURCHASE_STATUS, voucherTypeName } from '../utils/format'
import PurchaseDetail from './PurchaseDetail'
import PurchaseForm from './PurchaseForm'

// Fecha en el formato de los campos de fecha y de la API: 2026-10-20
const toIsoDate = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

// Si no se eligen fechas, se muestra el mes en curso: del día 1 a hoy.
const today = new Date()
const DEFAULT_FROM = toIsoDate(new Date(today.getFullYear(), today.getMonth(), 1))
const DEFAULT_TO = toIsoDate(today)

// Compras a proveedores (RF-15 / CU-23). Solo el administrador: acá se ven los saldos.
// Los filtros viven en la dirección, igual que en el historial de ventas.
export default function Purchases() {
  const [searchParams, setSearchParams] = useSearchParams()
  const from = searchParams.get('from') ?? DEFAULT_FROM
  const to = searchParams.get('to') ?? DEFAULT_TO
  const supplierId = searchParams.get('supplierId') ?? ''
  // Para responder lo que importa: a quién le debo.
  const soloConSaldo = searchParams.get('conSaldo') === 'si'

  const [purchases, setPurchases] = useState([])
  const [suppliers, setSuppliers] = useState([])
  const [formOpen, setFormOpen] = useState(false)
  // La factura que se esta mirando en la ficha, o null.
  const [selected, setSelected] = useState(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    getSuppliers('', false)
      .then(setSuppliers)
      .catch((e) => setError(e.message))
  }, [])

  const load = () => {
    getPurchases({ from, to, supplierId: supplierId || undefined })
      .then((data) => {
        setPurchases(data)
        setError('')
      })
      .catch((e) => {
        setPurchases([])
        setError(e.message)
      })
  }

  useEffect(() => {
    const timer = setTimeout(load, 300)
    return () => clearTimeout(timer)
  }, [from, to, supplierId])

  const setFilter = (name, value) => {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(name, value)
    else next.delete(name)
    setSearchParams(next, { replace: true })
  }

  const onSaved = (message) => {
    setFormOpen(false)
    setSuccess(message)
    load()
  }

  const mostradas = soloConSaldo ? purchases.filter((p) => Number(p.balance) > 0) : purchases

  // Los totales se calculan sobre lo que se está viendo.
  const totalAcordado = mostradas.reduce((suma, p) => suma + Number(p.agreedTotal), 0)
  const totalPagado = mostradas.reduce((suma, p) => suma + Number(p.paid), 0)
  const totalSaldo = totalAcordado - totalPagado

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Compras</h1>
          <p className="subtitle">Facturas de los proveedores y lo que se les debe.</p>
        </div>
        <button type="button" className="button button-primary" onClick={() => setFormOpen(true)}>
          + Cargar factura
        </button>
      </div>

      <div className="filters">
        <label className="filter">
          Desde
          <input type="date" value={from} max={to} onChange={(event) => setFilter('from', event.target.value)} />
        </label>
        <label className="filter">
          Hasta
          <input type="date" value={to} min={from} onChange={(event) => setFilter('to', event.target.value)} />
        </label>
        <label className="filter">
          Proveedor
          <select value={supplierId} onChange={(event) => setFilter('supplierId', event.target.value)}>
            <option value="">Todos</option>
            {suppliers.map((supplier) => (
              <option key={supplier.id} value={supplier.id}>{supplier.name}</option>
            ))}
          </select>
        </label>
        <label className="checkbox">
          <input type="checkbox" checked={soloConSaldo}
            onChange={(event) => setFilter('conSaldo', event.target.checked ? 'si' : '')} />
          Solo las que debo
        </label>
      </div>

      <Message type="error" text={error} onClose={() => setError('')} />
      <Message type="success" text={success} onClose={() => setSuccess('')} />

      <div className="cards">
        <div className="card">
          <span className="card-label">Comprado</span>
          <strong className="card-value">{formatPrice(totalAcordado)}</strong>
        </div>
        <div className="card">
          <span className="card-label">Pagado</span>
          <strong className="card-value">{formatPrice(totalPagado)}</strong>
        </div>
        <div className={totalSaldo > 0 ? 'card card-warning' : 'card'}>
          <span className="card-label">Saldo</span>
          <strong className="card-value">{formatPrice(totalSaldo)}</strong>
        </div>
      </div>

      {mostradas.length === 0 ? (
        <p className="notice">No hay facturas cargadas en este período.</p>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Comprobante</th>
                <th>Fecha</th>
                <th>Proveedor</th>
                <th className="right">Total</th>
                <th className="right">Pagado</th>
                <th className="right">Saldo</th>
                <th>Estado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {mostradas.map((purchase) => (
                <tr key={purchase.id}>
                  <td className="nowrap">
                    {purchase.voucherNumber}
                    <br />
                    <small className="help">{voucherTypeName(purchase.voucherType)}</small>
                  </td>
                  <td className="nowrap">{formatDate(purchase.date)}</td>
                  <td>{purchase.supplier?.name}</td>
                  <td className="right">
                    {formatPrice(purchase.agreedTotal)}
                    {/* Si se acordó pagar menos, se muestra lo facturado para poder compararlo con la foto. */}
                    {Number(purchase.agreedTotal) !== Number(purchase.invoicedTotal) && (
                      <>
                        <br />
                        <small className="help" title={purchase.adjustmentReason}>
                          facturado {formatPrice(purchase.invoicedTotal)}
                        </small>
                      </>
                    )}
                  </td>
                  <td className="right">{formatPrice(purchase.paid)}</td>
                  <td className={Number(purchase.balance) > 0 ? 'right low-stock' : 'right'}>
                    {formatPrice(purchase.balance)}
                  </td>
                  <td>
                    <span className={purchase.status === 'PAGADA' ? 'chip chip-active' : 'chip chip-inactive'}>
                      {PURCHASE_STATUS[purchase.status]}
                    </span>
                  </td>
                  <td className="actions">
                    <button type="button" className="button button-small" onClick={() => setSelected(purchase)}>
                      Ver
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {formOpen && <PurchaseForm onSaved={onSaved} onCancel={() => setFormOpen(false)} />}

      {/* onChanged: al registrar o anular un pago cambia el saldo, asi que el listado se recarga. */}
      {selected && (
        <PurchaseDetail purchase={selected} onChanged={load} onClose={() => setSelected(null)} />
      )}
    </>
  )
}
