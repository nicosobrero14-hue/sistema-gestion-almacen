import axios from 'axios'

// Cliente HTTP de toda la aplicacion. Vite redirige /api al backend.
const client = axios.create({ baseURL: '/api' })

// Todos los errores pasan por aca: cada pantalla recibe un mensaje listo para mostrar.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    const data = error.response?.data
    const appError = new Error(
      data?.message ?? 'No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.',
    )
    appError.errors = data?.errors ?? {} // error de cada campo, para marcarlo en el formulario
    return Promise.reject(appError)
  },
)

export default client
