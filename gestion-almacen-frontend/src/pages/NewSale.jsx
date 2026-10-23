import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  findProductByBarcode, getProducts, getProductsBySaleUnit, productImageUrl, suggestProduct,
} from '../api/products'
import { cancelPayment, checkPayment, saveSale } from '../api/sales'
import Message from '../components/Message'
import ProductImage from '../components/ProductImage'
import TextField from '../components/TextField'
import { isBarcode } from '../utils/barcode'
import { formatPrice, formatProductPrice, formatQuantity } from '../utils/format'
import ProductForm from './ProductForm'
import WeighPanel from './WeighPanel'

// El error de un renglon del carrito, o null si la cantidad esta bien (CU-08 exc. 3a y 4a).
const quantityError = (line) => {
  // Lo que se pesa admite menos de un kilo: 0,120 son 120 gramos. Lo que se cuenta, no.
  const porPeso = line.product.saleUnit === 'KG'
  const minimo = porPeso ? 0.001 : 1
  if (line.quantity === '' || Number(line.quantity) < minimo) {
    return porPeso ? 'Mínimo 0,001 kg' : 'Mínimo 1'
  }
  if (Number(line.quantity) > line.product.stock) {
    return `Hay ${formatQuantity(line.product.stock, line.product.saleUnit)} disponibles`
  }
  return null
}

// Nueva venta (RF-03 / CU-07 a CU-11): buscar productos, armar el carrito, elegir la forma de pago y confirmar.
// El carrito vive en esta pantalla. Al servidor le llega la venta completa cuando se confirma.
// isAdmin: lo necesita el formulario de producto, si hay que dar de alta uno que no existe.
export default function NewSale({ isAdmin }) {
  const navigate = useNavigate()

  const [search, setSearch] = useState('')
  const [results, setResults] = useState(null) // null = todavia no se busco nada

  // RF-12 / RF-14: el catalogo se muestra separado en dos, porque el circuito es distinto.
  // Lo que se cuenta se toca y va al carrito; lo que se pesa pasa antes por la balanza.
  const [byUnit, setByUnit] = useState(null) // null = todavia no cargo
  const [byWeight, setByWeight] = useState(null)
  // Tarjetas con la foto o la tabla de siempre. Se recuerda: despues de cada venta la pantalla
  // se arma de nuevo y volver a cambiarla cada vez seria molesto.
  const [view, setView] = useState(() => localStorage.getItem('vista-venta') ?? 'tarjetas')

  const [tab, setTab] = useState('unidad')
  // El producto que se esta pesando, o null.
  const [weighing, setWeighing] = useState(null)

  const changeView = (nueva) => {
    setView(nueva)
    localStorage.setItem('vista-venta', nueva)
  }
  const [cart, setCart] = useState([]) // renglones: { product, quantity }
  const [discount, setDiscount] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('EFECTIVO')
  const [cashReceived, setCashReceived] = useState('')
  // Venta esperando que el cliente pague con MercadoPago (CU-10). null = no hay ninguna esperando.
  const [waitingSaleId, setWaitingSaleId] = useState(null)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  // Codigo leido que no corresponde a ningun producto, y el formulario para darlo de alta (CU-03 paso 6).
  const [missingBarcode, setMissingBarcode] = useState('')
  const [productFormOpen, setProductFormOpen] = useState(false)
  // Datos que sugiere el catalogo publico para ese codigo, si lo conoce.
  const [suggestion, setSuggestion] = useState(null)

  // El buscador vuelve a tener el foco despues de cada producto agregado: el lector escribe donde esta el foco.
  const searchRef = useRef(null)

  useEffect(() => {
    getProductsBySaleUnit('UNIDAD')
      .then(setByUnit)
      .catch((e) => setError(e.message))
    getProductsBySaleUnit('KG')
      .then(setByWeight)
      .catch((e) => setError(e.message))
  }, [])

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

  // CU-10 pasos 5 y 6: mientras el cobro esta abierto se le pregunta a MercadoPago cada dos segundos.
  // El servidor es el que decide: esta pantalla solo mira como quedo la venta.
  useEffect(() => {
    if (waitingSaleId === null) return
    const timer = setInterval(async () => {
      let sale
      try {
        sale = await checkPayment(waitingSaleId)
      } catch (e) {
        return // un error puntual de red no corta la espera: se vuelve a preguntar
      }
      if (sale.status === 'CONFIRMADA') {
        setWaitingSaleId(null)
        navigate(`/sales/${sale.id}`, { state: { created: true } })
      } else if (sale.status === 'CANCELADA') {
        setWaitingSaleId(null)
        setSaving(false)
        setError('El cobro venció o se canceló. Puede intentar de nuevo o cobrar de otra forma.')
      }
    }, 2000)
    return () => clearInterval(timer)
  }, [waitingSaleId])

  // Agrega una unidad. Si el producto ya esta en el carrito, suma una mas a su renglon (CU-08).
  const addToCart = (product) => {
    const line = cart.find((item) => item.product.id === product.id)
    const quantity = line ? Number(line.quantity) + 1 : 1

    // CU-07 exc. 5a: no se puede superar el stock disponible.
    if (quantity > product.stock) {
      setError(`Solo hay ${formatQuantity(product.stock, product.saleUnit)} de "${product.name}".`)
      return
    }

    setError('')
    if (line) {
      changeQuantity(product.id, quantity)
    } else {
      setCart([...cart, { product, quantity }])
    }
    searchRef.current.focus()
  }

  const changeQuantity = (productId, quantity) => {
    setCart(cart.map((line) => (line.product.id === productId ? { ...line, quantity } : line)))
  }

  const removeFromCart = (productId) => setCart(cart.filter((line) => line.product.id !== productId))

  // Que productos se ven. Sin texto en el buscador, los que no se pueden escanear; con texto, lo
  // que devolvio la busqueda, que mira todo el catalogo. null en los dos casos es "todavia no llego".
  const searching = search.trim() !== ''
  const shown = tab === 'balanza' ? byWeight : (searching ? results : byUnit)

  // CU-21 pasos 3 y 4: con el peso confirmado se calcula el importe y entra al carrito.
  const addWeighed = (kilos) => {
    const product = weighing
    setWeighing(null)
    const line = cart.find((item) => item.product.id === product.id)
    if (line) {
      changeQuantity(product.id, kilos)
    } else {
      setCart([...cart, { product, quantity: kilos }])
    }
    searchRef.current?.focus()
  }

  // Un producto que se pesa no va directo al carrito: primero se pesa.
  const choose = (product) => {
    if (product.saleUnit === 'KG') setWeighing(product)
    else addToCart(product)
  }

  // CU-03 paso 4: en una venta, el codigo leido agrega el producto al carrito.
  const onSearchKeyDown = async (event) => {
    if (event.key !== 'Enter' || !isBarcode(search)) return
    setMissingBarcode('')
    try {
      const product = await findProductByBarcode(search.trim())
      if (!product.active) {
        setError(`"${product.name}" está dado de baja.`)
        return
      }
      addToCart(product)
      setSearch('') // el buscador queda listo para el proximo producto
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

  // El producto nuevo se agrega directo al carrito.
  const onProductCreated = (message, product) => {
    setProductFormOpen(false)
    setMissingBarcode('')
    setSuggestion(null)
    setSearch('')
    addToCart(product)
  }

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
      // Con MercadoPago la venta todavia no esta cobrada: queda esperando y el stock no se toco (CU-10).
      if (sale.status === 'PENDIENTE_PAGO') {
        setWaitingSaleId(sale.id)
        return
      }
      // CU-11 paso 7: la venta quedo registrada. Se muestra el ticket, y el carrito se vacia al salir de esta pantalla.
      navigate(`/sales/${sale.id}`, { state: { created: true } })
    } catch (e) {
      setError(e.message)
      setSaving(false)
    }
  }

  // CU-10 exc. 5a: el cliente se arrepiente o no llega a pagar.
  const cancelCharge = async () => {
    try {
      await cancelPayment(waitingSaleId)
    } catch (e) {
      // Puede haber pagado justo mientras se cancelaba: si quedo confirmada, se va al ticket.
      const sale = await checkPayment(waitingSaleId).catch(() => null)
      if (sale?.status === 'CONFIRMADA') {
        setWaitingSaleId(null)
        navigate(`/sales/${sale.id}`, { state: { created: true } })
        return
      }
      setError(e.message)
    }
    setWaitingSaleId(null)
    setSaving(false)
  }

  // Mientras se espera el pago no se puede hacer otra cosa: la pantalla muestra solo el cobro.
  if (waitingSaleId !== null) {
    return (
      <>
        <h1>Nueva venta</h1>
        <p className="subtitle">Cobro con MercadoPago.</p>

        <div className="panel waiting-payment">
          <h2>Esperando el pago</h2>
          <strong className="card-value">{formatPrice(total)}</strong>
          <p>El cliente tiene que escanear el código QR del mostrador y pagar desde su teléfono.</p>
          <p className="help">La venta se confirma sola apenas entra el pago.</p>
          <button type="button" className="button button-secondary" onClick={cancelCharge}>
            Cancelar el cobro
          </button>
        </div>
      </>
    )
  }

  return (
    <>
      <h1>Nueva venta</h1>
      <p className="subtitle">Busque los productos por nombre o código, o léalos con el lector, y agréguelos al carrito.</p>

      <Message type="error" text={error} onClose={() => setError('')} />
      <Message type="warning" text={missingBarcode && (suggestion
        ? `No hay ningún producto con el código ${missingBarcode}. Según el catálogo público es "${suggestion.name}".`
        : `No hay ningún producto con el código ${missingBarcode}.`)}
        onClose={() => { setMissingBarcode(''); setSuggestion(null) }}>
        <button type="button" className="button button-small" onClick={() => setProductFormOpen(true)}>
          Dar de alta con ese código
        </button>
      </Message>

      <div className="sale-layout">
        <section className="panel">
          <div className="panel-header">
            <h2>Productos</h2>
            {/* La lista entra mas productos en la pantalla y le sirve a quien ya se sabe el catalogo. */}
            <div className="view-toggle">
              <button type="button" className={view === 'tarjetas' ? 'active' : ''}
                onClick={() => changeView('tarjetas')} aria-pressed={view === 'tarjetas'}>
                Tarjetas
              </button>
              <button type="button" className={view === 'lista' ? 'active' : ''}
                onClick={() => changeView('lista')} aria-pressed={view === 'lista'}>
                Lista
              </button>
            </div>
          </div>
          {/* RF-12: el circuito de lo que se pesa es distinto al de lo que se cuenta. */}
          <div className="view-toggle tabs">
            <button type="button" className={tab === 'unidad' ? 'active' : ''}
              onClick={() => setTab('unidad')} aria-pressed={tab === 'unidad'}>
              Por unidad
            </button>
            <button type="button" className={tab === 'balanza' ? 'active' : ''}
              onClick={() => setTab('balanza')} aria-pressed={tab === 'balanza'}>
              Balanza
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
              ref={searchRef}
              autoFocus
            />
          </div>

          {shown !== null && shown.length > 0 && (
            <p className="help">
              {tab === 'balanza'
                ? 'Los que se venden por kilo. Al elegir uno se abre la ventana de pesaje.'
                : (!searching && 'Los que se venden por unidad. Con el lector se agregan sin buscarlos.')}
            </p>
          )}

          {/* CU-07 exc. 3a: el producto buscado no existe. Si lo leyo el lector, ya lo dice el aviso de arriba. */}
          {shown !== null && shown.length === 0 && !missingBarcode && (
            <p className="notice">
              {tab === 'balanza'
                ? 'No hay productos que se vendan por kilo.'
                : (searching
                  ? 'No hay ningún producto con ese nombre o código.'
                  : 'No hay productos cargados.')}
            </p>
          )}

          {shown !== null && shown.length > 0 && (view === 'tarjetas' ? (
            // RF-14: elegir el producto mirandolo, que es la unica forma comoda para lo que se vende suelto.
            <div className="product-grid">
              {shown.map((product) => (
                <button key={product.id} type="button" className="product-card"
                  onClick={() => choose(product)} disabled={product.stock === 0}>
                  <ProductImage src={productImageUrl(product.image)} name={product.name} large />
                  <span className="product-card-name">{product.name}</span>
                  <span className="product-card-price">
                    {product.onOffer ? (
                      <span className="offer-price">
                        <s>{formatProductPrice(product.price, product.saleUnit)}</s>{' '}
                        {formatProductPrice(product.salePrice, product.saleUnit)}
                      </span>
                    ) : (
                      formatProductPrice(product.salePrice, product.saleUnit)
                    )}
                  </span>
                  <span className={product.lowStock ? 'product-card-stock low-stock' : 'product-card-stock'}>
                    {product.stock === 0 ? 'Sin stock' : `${formatQuantity(product.stock, product.saleUnit)} disponibles`}
                  </span>
                </button>
              ))}
            </div>
          ) : (
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
                {shown.map((product) => (
                  <tr key={product.id}>
                    <td>{product.name}</td>
                    <td className="right">
                      {product.onOffer ? (
                        <span className="offer-price">
                          <s>{formatProductPrice(product.price, product.saleUnit)}</s>{' '}
                          {formatProductPrice(product.salePrice, product.saleUnit)}
                        </span>
                      ) : (
                        formatProductPrice(product.salePrice, product.saleUnit)
                      )}
                    </td>
                    <td className={product.lowStock ? 'right low-stock' : 'right'}>
                      {formatQuantity(product.stock, product.saleUnit)}
                    </td>
                    <td className="actions">
                      <button type="button" className="button button-small" onClick={() => choose(product)}
                        disabled={product.stock === 0}>
                        {product.stock === 0 ? 'Sin stock' : 'Agregar'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ))}
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
                    {/* El precio de lo que se pesa es por kilo y tiene que decirlo. */}
                    <td className="right">{formatProductPrice(line.product.salePrice, line.product.saleUnit)}</td>
                    <td>
                      <input
                        type="number"
                        min={line.product.saleUnit === 'KG' ? '0.001' : '1'}
                        max={line.product.stock}
                        step={line.product.saleUnit === 'KG' ? '0.001' : '1'}
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
              <label className="checkbox">
                <input type="radio" name="paymentMethod" value="MERCADOPAGO" checked={paymentMethod === 'MERCADOPAGO'}
                  onChange={(event) => setPaymentMethod(event.target.value)} />
                MercadoPago
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

            {/* CU-10: el cobro lo genera MercadoPago sobre el QR que esta pegado en el mostrador. */}
            {paymentMethod === 'MERCADOPAGO' && (
              <p className="help">Al confirmar se genera el cobro y el cliente paga escaneando el QR del mostrador.</p>
            )}

            {/* Deshabilitado mientras guarda: evita que un doble clic registre la venta dos veces. */}
            <button type="button" className="button button-primary button-wide" onClick={confirmSale} disabled={!canConfirm}>
              {saving ? 'Registrando...' : 'Confirmar venta'}
            </button>
          </div>
        </section>
      </div>

      {weighing && (
        <WeighPanel product={weighing} onConfirm={addWeighed} onCancel={() => setWeighing(null)} />
      )}

      {productFormOpen && (
        <ProductForm product={null} barcode={missingBarcode} suggestedName={suggestion?.name ?? ''}
          suggestedImage={suggestion?.imageUrl ?? ''} isAdmin={isAdmin} onSaved={onProductCreated}
          onCancel={() => setProductFormOpen(false)} />
      )}
    </>
  )
}
