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
