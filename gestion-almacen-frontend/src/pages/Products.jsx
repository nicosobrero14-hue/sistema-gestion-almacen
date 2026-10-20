import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  activateProduct, deactivateProduct, findProductByBarcode, getProducts, productImageUrl, suggestProduct,
} from '../api/products'
import Message from '../components/Message'
import ProductImage from '../components/ProductImage'
import { isBarcode } from '../utils/barcode'
import { formatPrice, formatProductPrice, formatQuantity } from '../utils/format'
import ProductForm from './ProductForm'

// Pantalla del catalogo de productos (RF-01 / CU-02, CU-20).
// isAdmin: el formulario bloquea el precio y la oferta si quien lo usa es empleado.
export default function Products({ isAdmin }) {
  // El lector escribe donde este el cursor: el buscador lo tiene desde que se abre la pantalla.
  const searchRef = useRef(null)
  // Las alertas del panel abren esta pantalla con el producto ya buscado: /products?search=Leche
  const [searchParams] = useSearchParams()

  const [products, setProducts] = useState([])
  const [search, setSearch] = useState(searchParams.get('search') ?? '')
  const [activeOnly, setActiveOnly] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Formulario: abierto o cerrado, y el producto que se edita (null = producto nuevo).
  const [formOpen, setFormOpen] = useState(false)
  const [selected, setSelected] = useState(null)

  // Codigo leido que no corresponde a ningun producto (CU-03 paso 6).
  const [missingBarcode, setMissingBarcode] = useState('')
  // Datos que sugiere el catalogo publico para ese codigo, si lo conoce.
  const [suggestion, setSuggestion] = useState(null)

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

  // Un alta comun empieza con el formulario vacio, sin el codigo ni la sugerencia de un escaneo anterior.
  const newProduct = () => {
    setMissingBarcode('')
    setSuggestion(null)
    openForm(null)
  }

  const volverAlBuscador = () => searchRef.current?.focus()

  const onSaved = (message) => {
    setFormOpen(false)
    setMissingBarcode('')
    setSuggestion(null)
    setSuccess(message)
    load()
    volverAlBuscador()
  }

  // CU-03 paso 5: el codigo leido filtra el listado y deja el producto a la vista con su precio.
  // No abre ninguna ficha: el catalogo funciona como consulta rapida de precios en el mostrador.
  const onSearchKeyDown = async (event) => {
    if (event.key !== 'Enter' || !isBarcode(search)) return
    setMissingBarcode('')
    setSuggestion(null)
    try {
      await findProductByBarcode(search.trim()) // existe: el listado ya lo esta mostrando
    } catch (e) {
      if (e.status !== 404) {
        setError(e.message)
        return
      }
      // CU-03 paso 6: si el codigo no existe, se ofrece darlo de alta y se le piden
      // al catalogo publico los datos del producto. Que no los tenga es normal.
      const code = search.trim()
      setMissingBarcode(code)
      suggestProduct(code).then(setSuggestion).catch(() => setSuggestion(null))
    }
  }

  const toggleActive = async (product) => {
    try {
      if (product.active) {
        // CU-02 exc. 3b: si todavia tiene stock, se avisa antes de darlo de baja.
        const question = product.stock > 0
          ? `"${product.name}" todavía tiene ${formatQuantity(product.stock, product.saleUnit)} en stock. ¿Darlo de baja igual?`
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
        <button type="button" className="button button-primary" onClick={newProduct}>
          + Nuevo producto
        </button>
      </div>

      <div className="filters">
        <input
          type="search"
          placeholder="Buscar por nombre o código de barras..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          onKeyDown={onSearchKeyDown}
          aria-label="Buscar productos"
          autoFocus
          ref={searchRef}
        />
        <label className="checkbox">
          <input type="checkbox" checked={activeOnly} onChange={(event) => setActiveOnly(event.target.checked)} />
          Ver solo activos
        </label>
      </div>

      <Message type="error" text={error} onClose={() => setError('')} />
      <Message type="success" text={success} onClose={() => setSuccess('')} />
      <Message type="warning" text={missingBarcode && (suggestion
        ? `No hay ningún producto con el código ${missingBarcode}. Según el catálogo público es "${suggestion.name}".`
        : `No hay ningún producto con el código ${missingBarcode}.`)}
        onClose={() => { setMissingBarcode(''); setSuggestion(null) }}>
        <button type="button" className="button button-small" onClick={() => openForm(null)}>
          Dar de alta con ese código
        </button>
      </Message>

      {products.length === 0 ? (
        <p className="notice">No se encontraron productos.</p>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Foto</th>
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
                  <td><ProductImage src={productImageUrl(product.image)} name={product.name} /></td>
                  <td>{product.name}</td>
                  <td>{product.barcode ?? '—'}</td>
                  <td className="right">
                    {product.onOffer ? (
                      <span className="offer-price">
                        <s>{formatProductPrice(product.price, product.saleUnit)}</s>{' '}
                        {formatProductPrice(product.offerPrice, product.saleUnit)}
                      </span>
                    ) : (
                      formatProductPrice(product.price, product.saleUnit)
                    )}
                  </td>
                  {/* Stock bajo: color y simbolo, para que se entienda sin distinguir colores (RNF-03). */}
                  <td className={product.lowStock ? 'right low-stock' : 'right'}>
                    {formatQuantity(product.stock, product.saleUnit)}{' '}
                    {product.lowStock && <span title="Stock por debajo del mínimo">⚠</span>}
                  </td>
                  <td className="right">{formatQuantity(product.minimumStock, product.saleUnit)}</td>
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

      {formOpen && (
        <ProductForm product={selected} barcode={missingBarcode} suggestedName={suggestion?.name ?? ''}
          suggestedImage={suggestion?.imageUrl ?? ''} isAdmin={isAdmin} onSaved={onSaved}
          onCancel={() => { setFormOpen(false); volverAlBuscador() }} />
      )}
    </>
  )
}
