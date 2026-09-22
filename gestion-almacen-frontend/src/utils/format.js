// Formatos que usan varias pantallas.

// 4850 -> "$ 4.850,00"
export const formatPrice = (value) => Number(value).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })

// "2026-09-21T17:40:12" -> "21/09/2026, 17:40"
export const formatDateTime = (value) => new Date(value).toLocaleString('es-AR', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false,
})

// "2026-10-15" -> "15/10/2026"
export const formatDate = (value) => value.split('-').reverse().join('/')

// Dias que faltan hasta una fecha como "2026-10-15". Negativo si ya paso.
export const daysUntil = (value) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((new Date(`${value}T00:00:00`) - today) / 86400000)
}

// 24 -> "Vence en 24 días"; -2 -> "Venció hace 2 días"
export const expirationText = (days) => {
  if (days < 0) return `Venció hace ${-days} ${days === -1 ? 'día' : 'días'}`
  if (days === 0) return 'Vence hoy'
  return `Vence en ${days} ${days === 1 ? 'día' : 'días'}`
}

// 15 -> "000015"
export const formatTicketNumber = (id) => String(id).padStart(6, '0')

export const PAYMENT_METHODS = {
  EFECTIVO: 'Efectivo',
  TRANSFERENCIA: 'Transferencia',
}
