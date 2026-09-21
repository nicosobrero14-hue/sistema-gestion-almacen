import client from './client'

// Llamadas a /api/stock.

export const adjustStock = (productId, adjustment) =>
  client.patch(`/stock/${productId}`, adjustment).then((response) => response.data)

export const getMovements = (productId) =>
  client.get(`/stock/${productId}/movements`).then((response) => response.data)

export const getLowStockProducts = () => client.get('/stock/low-stock').then((response) => response.data)

export const getExpiringProducts = (days) =>
  client.get('/stock/expiring', { params: { days } }).then((response) => response.data)
