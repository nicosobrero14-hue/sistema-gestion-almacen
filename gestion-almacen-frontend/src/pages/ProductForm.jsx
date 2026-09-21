import { useEffect, useState } from 'react'
import { editProduct, saveProduct } from '../api/products'
import { getSuppliers } from '../api/suppliers'
import Message from '../components/Message'
import Modal from '../components/Modal'
import TextField from '../components/TextField'

const EMPTY = {
  name: '',
  description: '',
  price: '',
  stock: 0,
  minimumStock: 0,
  barcode: '',
  expirationDate: '',
  onOffer: false,
  offerPrice: '',
  supplierId: '',
}

// Un campo vacio viaja como null, asi el backend avisa que es obligatorio en vez de recibir un cero.
const toNumber = (value) => (value === '' ? null : Number(value))

// Alta y modificacion de productos (CU-02, CU-20). Si product es null, es un alta.
// CU-19 y CU-20: el empleado pone el precio en el alta, pero despues solo el administrador lo cambia.
// Las ofertas son siempre del administrador. El backend controla lo mismo.
export default function ProductForm({ product, isAdmin, onSaved, onCancel }) {
  const canEditPrice = isAdmin || !product

  const [data, setData] = useState(
    product ? { ...product, supplierId: product.supplier?.id ?? '', offerPrice: product.offerPrice ?? '' } : EMPTY,
  )
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
      onOffer: data.onOffer,
      offerPrice: data.onOffer ? toNumber(data.offerPrice) : null,
      supplierId: toNumber(data.supplierId),
    }

    try {
      if (product) {
        await editProduct(product.id, body)
        onSaved(`"${body.name}" fue modificado.`)
      } else {
        await saveProduct(body)
        onSaved(`"${body.name}" fue creado.`)
      }
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
          error={errors.name} required maxLength={120} autoFocus />

        <TextField name="description" label="Descripción" value={data.description} onChange={onChange}
          error={errors.description} maxLength={255} />

        <div className="fields-row">
          <TextField name="price" label="Precio" type="number" step="0.01" min="0" value={data.price}
            onChange={onChange} error={errors.price} required disabled={!canEditPrice}
            help={canEditPrice ? null : 'Solo el administrador modifica precios'} />
          <TextField name="barcode" label="Código de barras" value={data.barcode} onChange={onChange}
            error={errors.barcode} maxLength={64} help="Opcional" />
        </div>

        <div className="fields-row">
          {/* El stock se carga solo en el alta. Despues se ajusta desde la pantalla de stock. */}
          <TextField name="stock" label={product ? 'Stock actual' : 'Stock inicial'} type="number" min="0" value={data.stock}
            onChange={onChange} error={errors.stock} required disabled={Boolean(product)}
            help={product ? 'Se ajusta desde la pantalla de stock' : null} />
          <TextField name="minimumStock" label="Stock mínimo" type="number" min="0" value={data.minimumStock}
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
