import client from './client'

// Llamadas a /api/products.

export const getProducts = (search, activeOnly) =>
  client.get('/products', { params: { search, activeOnly } }).then((response) => response.data)

// Codigo exacto, como lo manda el lector de codigo de barras (CU-03).
export const findProductByBarcode = (barcode) =>
  client.get(`/products/barcode/${encodeURIComponent(barcode)}`).then((response) => response.data)

export const saveProduct =(product) => client.post('/products', product).then((response) => response.data)

export const editProduct = (id, product) => client.put(`/products/${id}`, product).then((response) => response.data)

export const deactivateProduct = (id) => client.patch(`/products/${id}/deactivate`)

export const activateProduct = (id) => client.patch(`/products/${id}/activate`)
