import client from './client'

// Llamadas a /api/suppliers. Las pantallas usan estas funciones y no axios directamente.

export const getSuppliers = (search, activeOnly) =>
  client.get('/suppliers', { params: { search, activeOnly } }).then((response) => response.data)

export const saveSupplier = (supplier) => client.post('/suppliers', supplier).then((response) => response.data)

export const editSupplier = (id, supplier) => client.put(`/suppliers/${id}`, supplier).then((response) => response.data)

export const deactivateSupplier = (id) => client.patch(`/suppliers/${id}/deactivate`)

export const activateSupplier = (id) => client.patch(`/suppliers/${id}/activate`)
