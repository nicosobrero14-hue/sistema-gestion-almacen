import axios from 'axios'

// Cliente HTTP de toda la aplicacion. Vite redirige /api al backend.
const client = axios.create({ baseURL: '/api' })

export default client
