import client from './client'

// Llamadas a /api/purchases (RF-15).

export const getPurchases = (params) =>
  client.get('/purchases', { params }).then((response) => response.data)

export const findPurchase = (id) => client.get(`/purchases/${id}`).then((response) => response.data)

export const savePurchase = (purchase) => client.post('/purchases', purchase).then((response) => response.data)

// CU-23: la foto del comprobante. Va aparte del alta porque el nombre del archivo sale de su id.
export const uploadPurchaseImage = (id, file) => {
  const body = new FormData()
  body.append('file', file)
  return client.post(`/purchases/${id}/image`, body).then((response) => response.data)
}

export const deletePurchaseImage = (id) => client.delete(`/purchases/${id}/image`)

// La direccion desde la que el backend sirve la foto del comprobante.
export const purchaseImageUrl = (image) => (image ? `/imagenes/${image}` : '')

// CU-24: la plata que sale.
export const addPayment = (id, payment) =>
  client.post(`/purchases/${id}/payments`, payment).then((response) => response.data)

export const cancelPayment = (id, paymentId) =>
  client.patch(`/purchases/${id}/payments/${paymentId}/cancel`).then((response) => response.data)
