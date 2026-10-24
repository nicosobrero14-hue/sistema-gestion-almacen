import client from './client'

// Llamadas a /api/statistics.

// Los numeros del panel principal (RF-15 / CU-06).
// Que campos vienen depende del rol: al empleado el servidor no le manda la ganancia ni la
// deuda a proveedores, asi que la pantalla pregunta si el campo llego antes de mostrarlo.
export const getStatistics = () => client.get('/statistics').then((response) => response.data)
