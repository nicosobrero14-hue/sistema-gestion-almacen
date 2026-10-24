import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getProducts } from '../api/products'
import Message from '../components/Message'
import Pagination from '../components/Pagination'
import SortHeader from '../components/SortHeader'
import StockAdjustmentForm from './StockAdjustmentForm'
import StockMovements from './StockMovements'
import { formatQuantity } from '../utils/format'

// Cuantos productos entran en una pagina.
const POR_PAGINA = 20

// Pantalla de stock (RF-02 / CU-04): stock actual y minimo de cada producto, ajuste manual y movimientos.
export default function Stock() {
  // El lector escribe donde este el cursor: el buscador lo tiene desde que se abre la pantalla.
  const searchRef = useRef(null)
  // Las alertas del panel abren esta pantalla con el producto ya buscado: /stock?search=Leche
  const [searchParams] = useSearchParams()

  const [products, setProducts] = useState([])
  const [search, setSearch] = useState(searchParams.get('search') ?? '')
  const [lowStockOnly, setLowStockOnly] = useState(false)

  // Pagina y orden del listado (RF-13).
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [total, setTotal] = useState(0)
  const [sort, setSort] = useState('NOMBRE')
  const [direction, setDirection] = useState('ASC')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [warning, setWarning] = useState('')

  // El producto que se esta ajustando y el que se esta consultando. null = ventana cerrada.
  const [adjusting, setAdjusting] = useState(null)
  const [viewing, setViewing] = useState(null)

  // Solo los activos: a un producto dado de baja no se le ajusta el stock.
  // El filtro de stock bajo lo resuelve el backend. Filtrar aca solo la pagina que llego
  // escondia los productos con stock bajo que estaban en las otras.
  const load = () => {
    getProducts({ search, activeOnly: true, lowStockOnly, page, size: POR_PAGINA, sort, direction })
      .then((data) => {
        setProducts(data.content)
        setTotalPages(data.totalPages)
        setTotal(data.totalElements)
      })
      .catch((e) => setError(e.message))
  }

  // Vuelve a buscar cuando cambia el texto o el filtro. Espera 300 ms a que se termine de escribir.
  useEffect(() => {
    const timer = setTimeout(load, 300)
    return () => clearTimeout(timer)
  }, [search, lowStockOnly, page, sort, direction])

  // Un filtro nuevo empieza en la primera pagina.
  useEffect(() => {
    setPage(0)
  }, [search, lowStockOnly, sort, direction])

  // Clic en una columna: si ya se ordenaba por ella se da vuelta el sentido.
  const ordenarPor = (columna) => {
    if (columna === sort) {
      setDirection(direction === 'ASC' ? 'DESC' : 'ASC')
    } else {
      setSort(columna)
      setDirection('ASC')
    }
  }

  const onAdjusted = (product) => {
    setAdjusting(null)
    searchRef.current?.focus()
    setSuccess('')
    setWarning('')
    // CU-04 paso 7: si el stock quedo en el minimo o por debajo, se avisa que ya figura en las alertas.
    if (product.lowStock) {
      setWarning(`El stock de "${product.name}" quedó en ${formatQuantity(product.stock, product.saleUnit)}, por debajo del mínimo de ${formatQuantity(product.minimumStock, product.saleUnit)}. Ya figura en las alertas del panel principal.`)
    } else {
      setSuccess(`El stock de "${product.name}" quedó en ${product.stock}.`)
    }
    load()
  }

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
          autoFocus
          ref={searchRef}
        />
        <label className="checkbox">
          <input type="checkbox" checked={lowStockOnly} onChange={(event) => setLowStockOnly(event.target.checked)} />
          Ver solo stock bajo
        </label>
      </div>

      <Message type="error" text={error} onClose={() => setError('')} />
      <Message type="success" text={success} onClose={() => setSuccess('')} />
      <Message type="warning" text={warning} onClose={() => setWarning('')} />

      {products.length === 0 ? (
        <p className="notice">No se encontraron productos.</p>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <SortHeader column="NOMBRE" sort={sort} direction={direction} onSort={ordenarPor}>Producto</SortHeader>
                <SortHeader column="CODIGO" sort={sort} direction={direction} onSort={ordenarPor}>Código</SortHeader>
                <SortHeader column="STOCK" sort={sort} direction={direction} onSort={ordenarPor} className="right">Stock actual</SortHeader>
                <th className="right">Mínimo</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr key={product.id}>
                  <td>{product.name}</td>
                  <td>{product.barcode ?? '—'}</td>
                  <td className={product.lowStock ? 'right low-stock' : 'right'}>
                    {formatQuantity(product.stock, product.saleUnit)}{' '}
                    {product.lowStock && <span title="Stock por debajo del mínimo">⚠</span>}
                  </td>
                  <td className="right">{formatQuantity(product.minimumStock, product.saleUnit)}</td>
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

          <Pagination page={page} totalPages={totalPages} totalElements={total}
            name="producto" onChange={setPage} />
        </div>
      )}

      {adjusting && (
        <StockAdjustmentForm product={adjusting} onSaved={onAdjusted}
          onCancel={() => { setAdjusting(null); searchRef.current?.focus() }} />
      )}

      {viewing && <StockMovements product={viewing}
        onClose={() => { setViewing(null); searchRef.current?.focus() }} />}
    </>
  )
}
