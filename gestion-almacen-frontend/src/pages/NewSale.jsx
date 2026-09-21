import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getProducts } from '../api/products'
import { saveSale } from '../api/sales'
import Message from '../components/Message'
import TextField from '../components/TextField'

// Formato de moneda: 4850 -> "$ 4.850,00"
const formatPrice = (value) => Number(value).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })

// El error de un renglon del carrito, o null si la cantidad esta bien (CU-08 exc. 3a y 4a).
const quantityError = (line) => {
  if (line.quantity === '' || Number(line.quantity) < 1) return 'Mínimo 1'
  if (Number(line.quantity) > line.product.stock) return `Hay ${line.product.stock} disponibles`
  return null
}

// Nueva venta (RF-03 / CU-07 a CU-11): buscar productos, armar el carrito, elegir la forma de pago y confirmar.
// El carrito vive en esta pantalla. Al servidor le llega la venta completa cuando se confirma.
export default function NewSale() {
  const navigate = useNavigate()

  const [search, setSearch] = useState('')
  const [results, setResults] = useState(null) // null = todavia no se busco nada
  const [cart, setCart] = useState([]) // renglones: { product, quantity }
  const [discount, setDiscount] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('EFECTIVO')
  const [cashReceived, setCashReceived] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  // Busca mientras se escribe, 300 ms despues de la ultima tecla. Solo productos activos.
  useEffect(() => {
    if (search.trim() === '') {
      setResults(null)
      return
    }
    const timer = setTimeout(() => {
      getProducts(search, true)
        .then(setResults)
        .catch((e) => setError(e.message))
    }, 300)
    return () => clearTimeout(timer)
  }, [search])

  // Agrega una unidad. Si el producto ya esta en el carrito, suma una mas a su renglon (CU-08).
  const addToCart = (product) => {
    const line = cart.find((item) => item.product.id === product.id)
    const quantity = line ? Number(line.quantity) + 1 : 1

    // CU-07 exc. 5a: no se puede superar el stock disponible.
    if (quantity > product.stock) {
      setError(`Solo hay ${product.stock} unidades de "${product.name}".`)
      return
    }

    setError('')
    if (line) {
      changeQuantity(product.id, quantity)
    } else {
      setCart([...cart, { product, quantity }])
    }
  }

  const changeQuantity = (productId, quantity) => {
    setCart(cart.map((line) => (line.product.id === productId ? { ...line, quantity } : line)))
  }

  const removeFromCart = (productId) => setCart(cart.filter((line) => line.product.id !== productId))

  // Totales en pantalla. Los que valen son los que calcula el servidor al confirmar.
  const subtotal = cart.reduce((sum, line) => sum + line.product.salePrice * Number(line.quantity), 0)
  const discountValue = discount === '' ? 0 : Number(discount)
  const total = subtotal - discountValue
  const discountError = cart.length > 0 && (discountValue < 0 || discountValue >= subtotal)
    ? 'Tiene que ser menor al subtotal'
    : null

  // Vuelto: solo para efectivo, si se escribio con cuanto paga el cliente. Si no alcanza, es un error.
  const change = cashReceived === '' ? null : Number(cashReceived) - total
  const changeText = change === null ? 'Opcional: para calcular el vuelto' : `Vuelto: ${formatPrice(change)}`
  const cashError = paymentMethod === 'EFECTIVO' && change !== null && change < 0 ? `Faltan ${formatPrice(-change)}` : null

  // CU-11 exc. 1a: sin productos, o con algun dato mal, no se puede confirmar.
  const canConfirm = cart.length > 0 && cart.every((line) => !quantityError(line))
    && !discountError && !cashError && !saving

  const confirmSale = async () => {
    setSaving(true)
    setError('')
    try {
      const sale = await saveSale({
        items: cart.map((line) => ({ productId: line.product.id, quantity: Number(line.quantity) })),
        discount: discount === '' ? null : Number(discount),
        paymentMethod,
      })
      // CU-11 paso 7: la venta quedo registrada. Se muestra el ticket, y el carrito se vacia al salir de esta pantalla.
      navigate(`/sales/${sale.id}`, { state: { created: true } })
    } catch (e) {
      setError(e.message)
      setSaving(false)
    }
  }

  return (
    <>
      <h1>Nueva venta</h1>
      <p className="subtitle">Busque los productos por nombre o código de barras y agréguelos al carrito.</p>

      <Message type="error" text={error} onClose={() => setError('')} />

      <div className="sale-layout">
        <section className="panel">
          <h2>Productos</h2>
          <div className="filters">
            <input
              type="search"
              placeholder="Buscar por nombre o código de barras..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              aria-label="Buscar productos"
              autoFocus
            />
          </div>

          {/* CU-07 exc. 3a: el producto buscado no existe. */}
          {results !== null && results.length === 0 && (
            <p className="notice">No hay ningún producto con ese nombre o código.</p>
          )}

          {results !== null && results.length > 0 && (
            <table className="table">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th className="right">Precio</th>
                  <th className="right">Stock</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {results.map((product) => (
                  <tr key={product.id}>
                    <td>{product.name}</td>
                    <td className="right">
                      {product.onOffer ? (
                        <span className="offer-price">
                          <s>{formatPrice(product.price)}</s> {formatPrice(product.salePrice)}
                        </span>
                      ) : (
                        formatPrice(product.salePrice)
                      )}
                    </td>
                    <td className={product.lowStock ? 'right low-stock' : 'right'}>{product.stock}</td>
                    <td className="actions">
                      <button type="button" className="button button-small" onClick={() => addToCart(product)}
                        disabled={product.stock === 0}>
                        {product.stock === 0 ? 'Sin stock' : 'Agregar'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section className="panel">
          <h2>Carrito</h2>

          {cart.length === 0 ? (
            <p className="notice">El carrito está vacío. Busque un producto para empezar.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th className="right">Precio</th>
                  <th>Cantidad</th>
                  <th className="right">Subtotal</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {cart.map((line) => (
                  <tr key={line.product.id}>
                    <td>{line.product.name}</td>
                    <td className="right">{formatPrice(line.product.salePrice)}</td>
                    <td>
                      <input
                        type="number"
                        min="1"
                        max={line.product.stock}
                        className={quantityError(line) ? 'quantity has-error' : 'quantity'}
                        value={line.quantity}
                        onChange={(event) => changeQuantity(line.product.id, event.target.value)}
                        aria-label={`Cantidad de ${line.product.name}`}
                      />
                      {quantityError(line) && <small className="field-error">{quantityError(line)}</small>}
                    </td>
                    <td className="right">{formatPrice(line.product.salePrice * Number(line.quantity))}</td>
                    <td className="actions">
                      <button type="button" className="close-button" onClick={() => removeFromCart(line.product.id)}
                        aria-label={`Quitar ${line.product.name}`} title="Quitar del carrito">
                        &times;
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <div className="sale-summary">
            <div className="summary-row">
              <span>Subtotal</span>
              <strong>{formatPrice(subtotal)}</strong>
            </div>

            <TextField name="discount" label="Descuento en pesos" type="number" min="0" step="0.01"
              value={discount} onChange={(event) => setDiscount(event.target.value)} error={discountError}
              help="Opcional" />

            <div className="summary-row summary-total">
              <span>Total</span>
              <strong>{formatPrice(total)}</strong>
            </div>

            {/* CU-09: forma de pago. */}
            <fieldset className="payment-methods">
              <legend>Forma de pago</legend>
              <label className="checkbox">
                <input type="radio" name="paymentMethod" value="EFECTIVO" checked={paymentMethod === 'EFECTIVO'}
                  onChange={(event) => setPaymentMethod(event.target.value)} />
                Efectivo
              </label>
              <label className="checkbox">
                <input type="radio" name="paymentMethod" value="TRANSFERENCIA" checked={paymentMethod === 'TRANSFERENCIA'}
                  onChange={(event) => setPaymentMethod(event.target.value)} />
                Transferencia
              </label>
            </fieldset>

            {paymentMethod === 'EFECTIVO' && (
              <TextField name="cashReceived" label="Paga con" type="number" min="0" step="0.01"
                value={cashReceived} onChange={(event) => setCashReceived(event.target.value)}
                error={cashError} help={changeText} />
            )}

            {paymentMethod === 'TRANSFERENCIA' && (
              <p className="help">Confirme la venta cuando la transferencia figure acreditada.</p>
            )}

            {/* Deshabilitado mientras guarda: evita que un doble clic registre la venta dos veces. */}
            <button type="button" className="button button-primary button-wide" onClick={confirmSale} disabled={!canConfirm}>
              {saving ? 'Registrando...' : 'Confirmar venta'}
            </button>
          </div>
        </section>
      </div>
    </>
  )
}
