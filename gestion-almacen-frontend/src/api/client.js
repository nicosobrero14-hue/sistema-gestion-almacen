import axios from 'axios'

// Cliente HTTP de toda la aplicacion. Vite redirige /api al backend.
// withXSRFToken: en cada pedido que modifica datos manda el token CSRF que el backend dejo en la cookie.
const client = axios.create({ baseURL: '/api', withXSRFToken: true })

// Todos los errores pasan por aca: cada pantalla recibe un mensaje listo para mostrar.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const data = error.response?.data

    // 401 fuera del login: la sesion vencio. Se recarga y la aplicacion vuelve a pedir el login.
    if (status === 401 && !error.config.url.startsWith('/auth/')) {
      window.location.reload()
    }

    // El 403 de Spring Security llega sin mensaje; sin respuesta es que el servidor no contesto.
    const defaultMessage = status === 403
      ? 'No tiene permisos para realizar esta acción.'
      : 'No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.'

    const appError = new Error(data?.message ?? defaultMessage)
    appError.errors = data?.errors ?? {} // error de cada campo, para marcarlo en el formulario
    return Promise.reject(appError)
  },
)

export default client
