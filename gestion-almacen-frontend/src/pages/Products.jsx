import { useEffect, useState } from 'react'
import { activateProduct, deactivateProduct, getProducts } from '../api/products'
import Message from '../components/Message'
import ProductForm from './ProductForm'

// Formato de moneda: 4850 -> "$ 4.850,00"
const formatPrice = (value) => Number(value).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })

// Pantalla del catalogo de productos (RF-01 / CU-02, CU-20).
export default function Products() {
  const [products, setProducts] = useState([])
  const [search, setSearch] = useState('')
  const [activeOnly, setActiveOnly] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Formulario: abierto o cerrado, y el producto que se edita (null = producto nuevo).
  const [formOpen, setFormOpen] = useState(false)
  const [selected, setSelected] = useState(null)

  const load = () => {
    getProducts(search, activeOnly)
      .then(setProducts)
      .catch((e) => setError(e.message))
  }

  // Vuelve a buscar cuando cambia el texto o la casilla. Espera 300 ms a que se termine de escribir.
  useEffect(() => {
    const timer = setTimeout(load, 300)
    return () => clearTimeout(timer)
  }, [search, activeOnly])

  const openForm = (product) => {
    setSelected(product)
    setFormOpen(true)
  }

  const onSaved = (message) => {
    setFormOpen(false)
    setSuccess(message)
    load()
  }

  const toggleActive = async (product) => {
    try {
      if (product.active) {
        // CU-02 exc. 3b: si todavia tiene stock, se avisa antes de darlo de baja.
        const question = product.stock > 0
          ? `"${product.name}" todavía tiene ${product.stock} unidades en stock. ¿Darlo de baja igual?`
          : `¿Dar de baja "${product.name}"?`
        if (!window.confirm(question)) return
        await deactivateProduct(product.id)
        setSuccess(`"${product.name}" fue dado de baja.`)
      } else {
        await activateProduct(product.id)
        setSuccess(`"${product.name}" fue reactivado.`)
      }
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Productos</h1>
          <p className="subtitle">Catálogo del comercio.</p>
        </div>
        <button type="button" className="button button-primary" onClick={() => openForm(null)}>
          + Nuevo producto
        </button>
      </div>

      <div className="filters">
        <input
          type="search"
          placeholder="Buscar por nombre o código de barras..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          aria-label="Buscar productos"
        />
        <label className="checkbox">
          <input type="checkbox" checked={activeOnly} onChange={(event) => setActiveOnly(event.target.checked)} />
          Ver solo activos
        </label>
      </div>

      <Message type="error" text={error} onClose={() => setError('')} />
      <Message type="success" text={success} onClose={() => setSuccess('')} />

      {products.length === 0 ? (
        <p className="notice">No se encontraron productos.</p>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Producto</th>
                <th>Código</th>
                <th className="right">Precio</th>
                <th className="right">Stock</th>
                <th className="right">Mínimo</th>
                <th>Proveedor</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr key={product.id} className={product.active ? '' : 'inactive-row'}>
                  <td>{product.name}</td>
                  <td>{product.barcode ?? '—'}</td>
                  <td className="right">
                    {product.onOffer ? (
                      <span className="offer-price">
                        <s>{formatPrice(product.price)}</s> {formatPrice(product.offerPrice)}
                      </span>
                    ) : (
                      formatPrice(product.price)
                    )}
                  </td>
                  {/* Stock bajo: color y simbolo, para que se entienda sin distinguir colores (RNF-03). */}
                  <td className={product.lowStock ? 'right low-stock' : 'right'}>
                    {product.stock} {product.lowStock && <span title="Stock por debajo del mínimo">⚠</span>}
                  </td>
                  <td className="right">{product.minimumStock}</td>
                  <td>{product.supplier?.name ?? '—'}</td>
                  <td>
                    <span className={product.active ? 'chip chip-active' : 'chip chip-inactive'}>
                      {product.active ? 'Activo' : 'Dado de baja'}
                    </span>
                  </td>
                  <td className="actions">
                    <button type="button" className="button button-small" onClick={() => openForm(product)}>
                      Editar
                    </button>
                    <button type="button" className="button button-small button-secondary" onClick={() => toggleActive(product)}>
                      {product.active ? 'Dar de baja' : 'Reactivar'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {formOpen && <ProductForm product={selected} onSaved={onSaved} onCancel={() => setFormOpen(false)} />}
    </>
  )
}
