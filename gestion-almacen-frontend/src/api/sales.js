import client from './client'

// Llamadas a /api/sales.

export const saveSale = (sale) => client.post('/sales', sale).then((response) => response.data)

export const findSale = (id) => client.get(`/sales/${id}`).then((response) => response.data)

// Estado del cobro con MercadoPago (CU-10). Devuelve la venta, ya resuelta si el pago se definio.
export const checkPayment = (id) => client.get(`/sales/${id}/payment`).then((response) => response.data)

// Da de baja el cobro: el cliente ya no puede pagarlo (CU-10 exc. 5a).
export const cancelPayment = (id) => client.post(`/sales/${id}/payment/cancel`).then((response) => response.data)

// Historial (CU-16). filters: { from, to, username, product, reference }. Las fechas van como "2026-09-21".
export const getSales = (filters) => client.get('/sales', { params: filters }).then((response) => response.data)
