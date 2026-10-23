import client from './client'

// Llamadas a /api/products.

export const getProducts = (search, activeOnly) =>
  client.get('/products', { params: { search, activeOnly } }).then((response) => response.data)

// Los de una unidad de venta: UNIDAD para los que se cuentan, KG para los que se pesan.
// Cada pestania de la pantalla de venta muestra unos (RF-12 / RF-14).
export const getProductsBySaleUnit = (saleUnit) =>
  client.get(`/products/by-sale-unit/${saleUnit}`).then((response) => response.data)

// Codigo exacto, como lo manda el lector de codigo de barras (CU-03).
// CU-03 paso 6: datos sugeridos desde un catalogo publico para un codigo que no esta en el comercio.
export const suggestProduct = (barcode) =>
  client.get(`/products/suggestion/${barcode}`).then((response) => response.data)

export const findProductByBarcode = (barcode) =>
  client.get(`/products/barcode/${encodeURIComponent(barcode)}`).then((response) => response.data)

export const saveProduct =(product) => client.post('/products', product).then((response) => response.data)

export const editProduct = (id, product) => client.put(`/products/${id}`, product).then((response) => response.data)

export const deactivateProduct = (id) => client.patch(`/products/${id}/deactivate`)

export const activateProduct = (id) => client.patch(`/products/${id}/activate`)

// CU-22: la foto del producto. Va aparte del alta porque el nombre del archivo sale de su id.
export const uploadProductImage = (id, file) => {
  const body = new FormData()
  body.append('file', file)
  return client.post(`/products/${id}/image`, body).then((response) => response.data)
}

export const deleteProductImage = (id) => client.delete(`/products/${id}/image`)

// La direccion desde la que el backend sirve la foto. Vacio si el producto todavia no tiene.
export const productImageUrl = (image) => (image ? `/imagenes/${image}` : '')
