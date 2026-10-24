import client from './client'

// Llamadas a /api/sales.

export const saveSale = (sale) => client.post('/sales', sale).then((response) => response.data)

export const findSale = (id) => client.get(`/sales/${id}`).then((response) => response.data)

// Estado del cobro con MercadoPago (CU-10). Devuelve la venta, ya resuelta si el pago se definio.
export const checkPayment = (id) => client.get(`/sales/${id}/payment`).then((response) => response.data)

// Da de baja el cobro: el cliente ya no puede pagarlo (CU-10 exc. 5a).
export const cancelPayment = (id) => client.post(`/sales/${id}/payment/cancel`).then((response) => response.data)

// Historial (CU-16). filters: { from, to, username, product, reference, page, size }.
// Las fechas van como "2026-09-21". Devuelve una pagina: { content, page, totalElements, totalPages }.
export const getSales = (filters) => client.get('/sales', { params: filters }).then((response) => response.data)

// Resumen del periodo (CU-16 paso 6): { charged, total, byMethod }. Mismos filtros que el historial.
// Va aparte porque corresponde a todas las ventas del filtro y no a la pagina que se esta viendo.
export const getSalesSummary = (filters) =>
  client.get('/sales/summary', { params: filters }).then((response) => response.data)
