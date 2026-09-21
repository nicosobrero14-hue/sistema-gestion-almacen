import client from './client'

// Llamadas a /api/sales.

export const saveSale = (sale) => client.post('/sales', sale).then((response) => response.data)

export const findSale = (id) => client.get(`/sales/${id}`).then((response) => response.data)
