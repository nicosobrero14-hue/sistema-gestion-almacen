import client from './client'

// Llamadas a /api/offers. Solo el administrador.

export const getOffers = () => client.get('/offers').then((response) => response.data)

export const getLeastSoldProducts = (days) =>
  client.get('/offers/least-sold', { params: { days } }).then((response) => response.data)

export const putOnOffer = (productId, offer) => client.patch(`/offers/${productId}`, offer).then((response) => response.data)

export const endOffer = (productId) => client.patch(`/offers/${productId}/end`).then((response) => response.data)
