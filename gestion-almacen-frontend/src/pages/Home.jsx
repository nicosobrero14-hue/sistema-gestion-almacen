import { useEffect, useState } from 'react'
import client from '../api/client'

// Pantalla de inicio. Muestra si el backend y la base de datos responden.
export default function Home() {
  const [status, setStatus] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    client.get('/status')
      .then((response) => setStatus(response.data))
      .catch(() => setError('No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.'))
  }, [])

  return (
    <>
      <h1>Panel principal</h1>
      <p className="subtitle">Estado del sistema.</p>

      {error && <p className="alert">{error}</p>}

      {status && (
        <div className="cards">
          <div className="card">
            <span className="card-label">Aplicación</span>
            <strong className="card-value">{status.application}</strong>
          </div>
          <div className="card">
            <span className="card-label">Base de datos</span>
            <strong className="card-value">{status.database}</strong>
          </div>
        </div>
      )}
    </>
  )
}
