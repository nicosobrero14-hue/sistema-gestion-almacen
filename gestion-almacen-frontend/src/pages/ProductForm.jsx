import { useEffect, useState } from 'react'
import {
  deleteProductImage, editProduct, productImageUrl, saveProduct, suggestProduct, uploadProductImage,
} from '../api/products'
import { getSuppliers } from '../api/suppliers'
import Message from '../components/Message'
import Modal from '../components/Modal'
import ProductImage from '../components/ProductImage'
import TextField from '../components/TextField'
import { isBarcode } from '../utils/barcode'
import { SALE_UNITS } from '../utils/format'

const EMPTY = {
  name: '',
  description: '',
  price: '',
  stock: 0,
  minimumStock: 0,
  saleUnit: 'UNIDAD',
  barcode: '',
  expirationDate: '',
  onOffer: false,
  offerPrice: '',
  supplierId: '',
  image: null,   // nombre del archivo que ya tiene guardado el producto
  imageUrl: '',  // enlace a la foto que sugirio el catalogo publico, solo en un alta
}

// Un campo vacio viaja como null, asi el backend avisa que es obligatorio en vez de recibir un cero.
const toNumber = (value) => (value === '' ? null : Number(value))

// Alta y modificacion de productos (CU-02, CU-20). Si product es null, es un alta.
// CU-19 y CU-20: el empleado pone el precio en el alta, pero despues solo el administrador lo cambia.
// Las ofertas son siempre del administrador. El backend controla lo mismo.
// barcode: en un alta, el codigo que leyo el lector y no existia (CU-03 paso 6).
// suggestedName: el nombre que sugirio el catalogo publico para ese codigo, si lo conocia.
// suggestedImage: el enlace a la foto de ese mismo catalogo. El backend la descarga al guardar.
// onSaved recibe el mensaje y el producto guardado.
export default function ProductForm({ product, barcode = '', suggestedName = '', suggestedImage = '',
  isAdmin, onSaved, onCancel }) {
  const canEditPrice = isAdmin || !product

  const [data, setData] = useState(
    product
      ? { ...product, supplierId: product.supplier?.id ?? '', offerPrice: product.offerPrice ?? '' }
      : { ...EMPTY, barcode, name: suggestedName, imageUrl: suggestedImage },
  )

  // Foto elegida desde el disco, todavia sin subir, y la orden de quitar la que ya tenia.
  // Las dos se aplican recien al guardar: cancelar el formulario no tiene que cambiar nada.
  const [file, setFile] = useState(null)
  const [removeImage, setRemoveImage] = useState(false)
  const [filePreview, setFilePreview] = useState('')

  // La vista previa de un archivo del disco es una direccion temporal que hay que liberar,
  // o el navegador se queda con la imagen en memoria hasta recargar la pagina.
  useEffect(() => {
    if (!file) {
      setFilePreview('')
      return undefined
    }
    const temporal = URL.createObjectURL(file)
    setFilePreview(temporal)
    return () => URL.revokeObjectURL(temporal)
  }, [file])

  // Que foto se muestra: la que se acaba de elegir, la que ya tenia, o la que sugirio el catalogo.
  const preview = filePreview || (removeImage ? '' : productImageUrl(data.image) || data.imageUrl)
  // Por peso cambian las etiquetas y los campos admiten decimales.
  const porPeso = data.saleUnit === 'KG'
  // Si el nombre lo sugirio el catalogo publico, se avisa para que se revise.
  const [suggested, setSuggested] = useState(Boolean(suggestedName))

  // CU-03: el lector escribe donde este el cursor, y lo normal es que sea el nombre.
  // Si lo que entro es un codigo de barras, se lo pasa a su campo y se piden los datos.
  const onNameKeyDown = async (event) => {
    if (event.key !== 'Enter' || !isBarcode(data.name)) return
    event.preventDefault() // Enter en un formulario lo enviaria
    const code = data.name.trim()
    setData((actual) => ({ ...actual, name: '', barcode: code }))
    try {
      const found = await suggestProduct(code)
      setData((actual) => ({ ...actual, name: found.name, imageUrl: found.imageUrl ?? '' }))
      setSuggested(true)
    } catch (e) {
      setSuggested(false) // que el catalogo no lo conozca es normal: se escribe a mano
    }
  }

  // Se avisa en el momento y no despues de subirla: el backend valida lo mismo.
  const onFileChange = (event) => {
    const elegido = event.target.files[0]
    if (!elegido) return

    if (!['image/jpeg', 'image/png', 'image/webp'].includes(elegido.type)) {
      setError('La imagen tiene que ser JPG, PNG o WEBP.')
      return
    }
    if (elegido.size > 2 * 1024 * 1024) {
      setError('La imagen no puede superar los 2 MB.')
      return
    }
    setError('')
    setRemoveImage(false)
    setFile(elegido)
  }

  const quitarFoto = () => {
    setFile(null)
    setRemoveImage(true)
    // Tambien se descarta la que sugirio el catalogo, o se descargaria igual al guardar.
    setData((actual) => ({ ...actual, imageUrl: '' }))
  }

  const [suppliers, setSuppliers] = useState([])
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  // Solo los proveedores activos: el backend no deja asignar uno dado de baja.
  useEffect(() => {
    getSuppliers('', true)
      .then(setSuppliers)
      .catch((e) => setError(e.message))
  }, [])

  // Un solo manejador para todos los campos: usa el "name" del input.
  const onChange = (event) => {
    const { name, type, value, checked } = event.target
    setData({ ...data, [name]: type === 'checkbox' ? checked : value })
  }

  const onSubmit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    setError('')

    const body = {
      name: data.name,
      description: data.description || null,
      price: toNumber(data.price),
      stock: toNumber(data.stock),
      minimumStock: toNumber(data.minimumStock),
      barcode: data.barcode || null,
      expirationDate: data.expirationDate || null,
      // Solo en el alta: el backend descarga esa foto y guarda el archivo (CU-03 paso 6).
      imageUrl: product ? null : data.imageUrl || null,
      onOffer: data.onOffer,
      offerPrice: data.onOffer ? toNumber(data.offerPrice) : null,
      supplierId: toNumber(data.supplierId),
    }

    try {
      const saved = product ? await editProduct(product.id, body) : await saveProduct(body)
      let aviso = product ? `"${body.name}" fue modificado.` : `"${body.name}" fue creado.`

      // La foto viaja en un pedido aparte: el nombre del archivo sale del id del producto, que
      // recien existe una vez guardado. Si falla, el producto igual quedo bien y se avisa.
      try {
        if (file) await uploadProductImage(saved.id, file)
        else if (removeImage && product?.image) await deleteProductImage(saved.id)
      } catch (e) {
        aviso += ` La foto no se pudo guardar: ${e.message}`
      }
      onSaved(aviso, saved)
    } catch (e) {
      setErrors(e.errors)
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={product ? `Editar producto: ${product.name}` : 'Nuevo producto'} onClose={onCancel}>
      <Message text={error} onClose={() => setError('')} />

      <form onSubmit={onSubmit} noValidate>
        <TextField name="name" label="Nombre" value={data.name} onChange={onChange}
          onKeyDown={product ? undefined : onNameKeyDown}
          help={suggested && !product ? 'Sugerido por el catálogo público: revíselo antes de guardar' : null}
          error={errors.name} required maxLength={120} autoFocus />

        <TextField name="description" label="Descripción" value={data.description} onChange={onChange}
          error={errors.description} maxLength={255} />

        <div className="fields-row">
          {/* RF-12: define si el precio es por unidad o por kilo, y como se carga en una venta. */}
          <div className="field">
            <label htmlFor="saleUnit">Unidad de venta</label>
            <select id="saleUnit" name="saleUnit" value={data.saleUnit} onChange={onChange}>
              {Object.keys(SALE_UNITS).map((unit) => (
                <option key={unit} value={unit}>{SALE_UNITS[unit]}</option>
              ))}
            </select>
            <small className="help">Por kilo: se pesa al venderlo</small>
          </div>
          <TextField name="price" label={porPeso ? 'Precio por kilo' : 'Precio'} type="number" step="0.01" min="0"
            value={data.price} onChange={onChange} error={errors.price} required disabled={!canEditPrice}
            help={canEditPrice ? null : 'Solo el administrador modifica precios'} />
        </div>

        <div className="fields-row">
          <TextField name="barcode" label="Código de barras" value={data.barcode} onChange={onChange}
            error={errors.barcode} maxLength={64} help="Opcional" />
        </div>

        {/* CU-22: la foto con la que el producto se reconoce de un vistazo en la pantalla de venta. */}
        <div className="image-field">
          <ProductImage src={preview} name={data.name} large />
          <div>
            <label htmlFor="image">Foto del producto</label>
            <input id="image" type="file" accept="image/jpeg,image/png,image/webp" onChange={onFileChange} />
            <small className="help">
              {data.imageUrl && !file && !data.image
                ? 'La trajo el catálogo público al leer el código. Se guarda junto con el producto.'
                : 'JPG, PNG o WEBP de hasta 2 MB. Mejor con fondo claro y el producto centrado.'}
            </small>
            {preview && (
              <button type="button" className="button button-small button-secondary" onClick={quitarFoto}>
                Quitar foto
              </button>
            )}
          </div>
        </div>

        <div className="fields-row">
          {/* El stock se carga solo en el alta. Despues se ajusta desde la pantalla de stock. */}
          <TextField name="stock" label={(product ? 'Stock actual' : 'Stock inicial') + (porPeso ? ' en kg' : '')}
            type="number" min="0" step={porPeso ? '0.001' : '1'} value={data.stock}
            onChange={onChange} error={errors.stock} required disabled={Boolean(product)}
            help={product ? 'Se ajusta desde la pantalla de stock' : null} />
          <TextField name="minimumStock" label={'Stock mínimo' + (porPeso ? ' en kg' : '')}
            type="number" min="0" step={porPeso ? '0.001' : '1'} value={data.minimumStock}
            onChange={onChange} error={errors.minimumStock} required help="Umbral de la alerta de stock bajo" />
        </div>

        <div className="fields-row">
          <TextField name="expirationDate" label="Fecha de vencimiento" type="date" value={data.expirationDate}
            onChange={onChange} error={errors.expirationDate} help="Solo productos perecederos" />

          <div className="field">
            <label htmlFor="supplierId">Proveedor</label>
            <select id="supplierId" name="supplierId" value={data.supplierId} onChange={onChange}>
              <option value="">Sin proveedor</option>
              {suppliers.map((supplier) => (
                <option key={supplier.id} value={supplier.id}>
                  {supplier.name} {supplier.lastName}
                </option>
              ))}
            </select>
          </div>
        </div>

        <label className="checkbox">
          <input type="checkbox" name="onOffer" checked={data.onOffer} onChange={onChange} disabled={!isAdmin} />
          Producto en oferta
          {!isAdmin && <small className="help">(las ofertas las define el administrador)</small>}
        </label>

        {data.onOffer && (
          <TextField name="offerPrice" label="Precio de oferta" type="number" step="0.01" min="0"
            value={data.offerPrice} onChange={onChange} error={errors.offerPrice} required disabled={!isAdmin}
            help="Tiene que ser menor al precio normal" />
        )}

        <div className="form-actions">
          <button type="button" className="button button-secondary" onClick={onCancel}>Cancelar</button>
          {/* Deshabilitado mientras guarda: evita que un doble clic lo mande dos veces. */}
          <button type="submit" className="button button-primary" disabled={saving}>
            {saving ? 'Guardando...' : 'Guardar'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
