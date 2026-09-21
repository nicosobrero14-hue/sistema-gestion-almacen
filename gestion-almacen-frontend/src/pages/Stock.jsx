import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getProducts } from '../api/products'
import Message from '../components/Message'
import StockAdjustmentForm from './StockAdjustmentForm'
import StockMovements from './StockMovements'

// Pantalla de stock (RF-02 / CU-04): stock actual y minimo de cada producto, ajuste manual y movimientos.
export default function Stock() {
  // Las alertas del panel abren esta pantalla con el producto ya buscado: /stock?search=Leche
  const [searchParams] = useSearchParams()

  const [products, setProducts] = useState([])
  const [search, setSearch] = useState(searchParams.get('search') ?? '')
  const [lowStockOnly, setLowStockOnly] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [warning, setWarning] = useState('')

  // El producto que se esta ajustando y el que se esta consultando. null = ventana cerrada.
  const [adjusting, setAdjusting] = useState(null)
  const [viewing, setViewing] = useState(null)

  // Solo los activos: a un producto dado de baja no se le ajusta el stock.
  const load = () => {
    getProducts(search, true)
      .then(setProducts)
      .catch((e) => setError(e.message))
  }

  // Vuelve a buscar cuando cambia el texto. Espera 300 ms a que se termine de escribir.
  useEffect(() => {
    const timer = setTimeout(load, 300)
    return () => clearTimeout(timer)
  }, [search])

  const onAdjusted = (product) => {
    setAdjusting(null)
    setSuccess('')
    setWarning('')
    // CU-04 paso 7: si el stock quedo en el minimo o por debajo, se avisa que ya figura en las alertas.
    if (product.lowStock) {
      setWarning(`El stock de "${product.name}" quedó en ${product.stock}, por debajo del mínimo de ${product.minimumStock}. Ya figura en las alertas del panel principal.`)
    } else {
      setSuccess(`El stock de "${product.name}" quedó en ${product.stock}.`)
    }
    load()
  }

  const visibleProducts = lowStockOnly ? products.filter((product) => product.lowStock) : products

  return (
    <>
      <h1>Stock</h1>
      <p className="subtitle">Stock actual de cada producto. Cada ajuste queda registrado con su motivo.</p>

      <div className="filters">
        <input
          type="search"
          placeholder="Buscar por nombre o código de barras..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          aria-label="Buscar productos"
        />
        <label className="checkbox">
          <input type="checkbox" checked={lowStockOnly} onChange={(event) => setLowStockOnly(event.target.checked)} />
          Ver solo stock bajo
        </label>
      </div>

      <Message type="error" text={error} onClose={() => setError('')} />
      <Message type="success" text={success} onClose={() => setSuccess('')} />
      <Message type="warning" text={warning} onClose={() => setWarning('')} />

      {visibleProducts.length === 0 ? (
        <p className="notice">No se encontraron productos.</p>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Producto</th>
                <th>Código</th>
                <th className="right">Stock actual</th>
                <th className="right">Mínimo</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {visibleProducts.map((product) => (
                <tr key={product.id}>
                  <td>{product.name}</td>
                  <td>{product.barcode ?? '—'}</td>
                  <td className={product.lowStock ? 'right low-stock' : 'right'}>
                    {product.stock} {product.lowStock && <span title="Stock por debajo del mínimo">⚠</span>}
                  </td>
                  <td className="right">{product.minimumStock}</td>
                  <td>
                    <span className={product.lowStock ? 'chip chip-warning' : 'chip chip-active'}>
                      {product.lowStock ? 'Stock bajo' : 'Normal'}
                    </span>
                  </td>
                  <td className="actions">
                    <button type="button" className="button button-small" onClick={() => setAdjusting(product)}>
                      Ajustar
                    </button>
                    <button type="button" className="button button-small button-secondary" onClick={() => setViewing(product)}>
                      Movimientos
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {adjusting && (
        <StockAdjustmentForm product={adjusting} onSaved={onAdjusted} onCancel={() => setAdjusting(null)} />
      )}

      {viewing && <StockMovements product={viewing} onClose={() => setViewing(null)} />}
    </>
  )
}
