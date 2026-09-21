// Formatos que usan varias pantallas.

// 4850 -> "$ 4.850,00"
export const formatPrice = (value) => Number(value).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })

// "2026-09-21T17:40:12" -> "21/09/2026, 17:40"
export const formatDateTime = (value) => new Date(value).toLocaleString('es-AR', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false,
})

// "2026-10-15" -> "15/10/2026"
export const formatDate = (value) => value.split('-').reverse().join('/')

// 15 -> "000015"
export const formatTicketNumber = (id) => String(id).padStart(6, '0')

export const PAYMENT_METHODS = {
  EFECTIVO: 'Efectivo',
  TRANSFERENCIA: 'Transferencia',
}
