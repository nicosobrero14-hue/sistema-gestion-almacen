import { useEffect, useRef, useState } from 'react'
import { readWeight } from '../api/scale'
import Modal from '../components/Modal'
import ProductImage from '../components/ProductImage'
import { productImageUrl } from '../api/products'
import { formatPrice, formatProductPrice } from '../utils/format'

// Cada cuánto se le pregunta el peso a la balanza. La Kretz transmite dos veces por segundo,
// así que preguntar más seguido no agrega nada.
const CADA_MS = 500

// Ventana de pesaje (RF-12 / CU-21).
// El peso que vale es el que estaba en pantalla cuando el usuario confirmó, no uno leído en ese
// instante: entre que mira el número y aprieta el botón, la mercadería se puede mover.
// onConfirm recibe el peso en kilos.
export default function WeighPanel({ product, onConfirm, onCancel }) {
  // null mientras no se sabe, false cuando se confirmó que no hay balanza conectada.
  const [hayBalanza, setHayBalanza] = useState(null)
  const [peso, setPeso] = useState('')
  const manualRef = useRef(null)

  // CU-21 paso 2: se le pregunta el peso a la balanza y se muestra, actualizándose mientras
  // el usuario acomoda la mercadería.
  useEffect(() => {
    let vivo = true

    const preguntar = async () => {
      try {
        const leido = await readWeight()
        if (!vivo) return
        if (leido === null || leido === undefined) {
          setHayBalanza(false)
          return
        }
        setHayBalanza(true)
        setPeso(String(leido))
      } catch (e) {
        if (vivo) setHayBalanza(false)
      }
    }

    preguntar()
    const timer = setInterval(preguntar, CADA_MS)
    return () => {
      vivo = false
      clearInterval(timer)
    }
  }, [])

  // CU-21 exc. 2a: sin balanza se escribe el peso a mano. Un comercio no puede dejar de vender
  // porque se desconectó un cable.
  useEffect(() => {
    if (hayBalanza === false) manualRef.current?.focus()
  }, [hayBalanza])

  const kilos = Number(peso)
  const valido = peso !== '' && kilos > 0 && kilos <= Number(product.stock)
  const importe = valido ? kilos * Number(product.salePrice) : 0

  const confirmar = () => {
    if (valido) onConfirm(kilos)
  }

  // F2 confirma sin soltar la mercadería. Enter hace lo mismo para quien venga del teclado.
  useEffect(() => {
    const onKey = (event) => {
      if (event.key === 'F2' || event.key === 'Enter') {
        event.preventDefault()
        confirmar()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  })

  return (
    <Modal title={`Pesar ${product.name}`} onClose={onCancel}>
      <div className="weigh">
        <ProductImage src={productImageUrl(product.image)} name={product.name} large />

        <div className="weigh-main">
          {/* El visor, con el número grande como el de la balanza. */}
          <div className="weigh-display">
            <span className="weigh-kilos">{peso === '' ? '0.000' : peso}</span>
            <span className="weigh-unit">kg</span>
          </div>

          <p className="help">
            {hayBalanza === null && 'Buscando la balanza...'}
            {hayBalanza === true && 'Peso en vivo desde la balanza. Acomode la mercadería y confirme.'}
            {hayBalanza === false && 'No hay balanza conectada: escriba el peso a mano.'}
          </p>

          {/* Siempre editable: aunque haya balanza, el empleado puede corregir. */}
          <input
            ref={manualRef}
            type="number"
            step="0.001"
            min="0"
            value={peso}
            onChange={(event) => setPeso(event.target.value)}
            aria-label="Peso en kilos"
            className="weigh-input"
          />

          <p className="weigh-amount">
            {formatProductPrice(product.salePrice, 'KG')} · <strong>{formatPrice(importe)}</strong>
          </p>

          {peso !== '' && kilos > Number(product.stock) && (
            <p className="field-error">Solo hay {product.stock} kg de {product.name}.</p>
          )}
        </div>
      </div>

      <div className="form-actions">
        <button type="button" className="button button-secondary" onClick={onCancel}>Cancelar</button>
        <button type="button" className="button button-primary" onClick={confirmar} disabled={!valido}>
          Confirmar peso (F2)
        </button>
      </div>
    </Modal>
  )
}
