import { useEffect, useState } from 'react'
import { getMovements } from '../api/stock'
import Message from '../components/Message'
import Modal from '../components/Modal'
import { formatDateTime } from '../utils/format'

const TYPES = {
  CARGA_INICIAL: 'Carga inicial',
  AJUSTE_MANUAL: 'Ajuste manual',
  VENTA: 'Venta',
}

// Movimientos de stock de un producto (RF-02): quien cambio el stock, cuando y por que.
export default function StockMovements({ product, onClose }) {
  const [movements, setMovements] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    getMovements(product.id)
      .then(setMovements)
      .catch((e) => setError(e.message))
  }, [product.id])

  return (
    <Modal title={`Movimientos: ${product.name}`} onClose={onClose} wide>
      <Message text={error} onClose={() => setError('')} />

      {movements.length === 0 ? (
        <p className="notice">Este producto todavía no tiene movimientos.</p>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Tipo</th>
                <th className="right">Anterior</th>
                <th className="right">Nuevo</th>
                <th className="right">Diferencia</th>
                <th>Usuario</th>
                <th>Motivo</th>
              </tr>
            </thead>
            <tbody>
              {movements.map((movement) => {
                const difference = movement.newStock - movement.previousStock
                return (
                  <tr key={movement.id}>
                    <td className="nowrap">{formatDateTime(movement.dateTime)}</td>
                    <td>{TYPES[movement.type]}</td>
                    <td className="right">{movement.previousStock}</td>
                    <td className="right">{movement.newStock}</td>
                    <td className={difference > 0 ? 'right positive' : 'right negative'}>
                      {difference > 0 ? '+' : ''}{difference}
                    </td>
                    <td>{movement.username}</td>
                    <td>{movement.reason}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </Modal>
  )
}
