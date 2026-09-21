import client from './client'

// Llamadas a /api/users.

export const getUsers = (search, activeOnly) =>
  client.get('/users', { params: { search, activeOnly } }).then((response) => response.data)

export const saveUser = (user) => client.post('/users', user).then((response) => response.data)

// Si password va en null, el backend conserva la contraseña actual.
export const editUser = (id, user) => client.put(`/users/${id}`, user).then((response) => response.data)

export const deactivateUser = (id) => client.patch(`/users/${id}/deactivate`)

export const activateUser = (id) => client.patch(`/users/${id}/activate`)
