import client from './client'

// Lectura del peso de la balanza (RF-12 / CU-21).
// Devuelve el peso en kilos, o null si este equipo no tiene balanza conectada. No es un error:
// sin balanza el peso se escribe a mano y el comercio sigue vendiendo.
export const readWeight = () => client.get('/scale/weight').then((response) => response.data.weight)
