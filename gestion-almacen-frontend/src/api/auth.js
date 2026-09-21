import client from './client'

// Inicio y cierre de sesion (CU-01).

export const login = (username, password) =>
  client.post('/auth/login', { username, password }).then((response) => response.data)

export const logout = () => client.post('/auth/logout')

// Quien esta conectado. Si no hay sesion, el backend responde 401.
export const getSessionUser = () => client.get('/auth/me').then((response) => response.data)
