import client from './client'

// Llamadas a /api/sales.

export const saveSale = (sale) => client.post('/sales', sale).then((response) => response.data)

export const findSale = (id) => client.get(`/sales/${id}`).then((response) => response.data)

// Historial (CU-16). filters: { from, to, username, product }. Las fechas van como "2026-09-21".
export const getSales = (filters) => client.get('/sales', { params: filters }).then((response) => response.data)
