import client from './client'

// Llamadas a /api/products.

export const getProducts = (search, activeOnly) =>
  client.get('/products', { params: { search, activeOnly } }).then((response) => response.data)

export const saveProduct = (product) => client.post('/products', product).then((response) => response.data)

export const editProduct = (id, product) => client.put(`/products/${id}`, product).then((response) => response.data)

export const deactivateProduct = (id) => client.patch(`/products/${id}/deactivate`)

export const activateProduct = (id) => client.patch(`/products/${id}/activate`)
