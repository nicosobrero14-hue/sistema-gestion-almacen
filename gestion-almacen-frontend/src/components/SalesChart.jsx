import { formatPrice } from '../utils/format'

// Grafico de lo vendido en los ultimos dias (RF-15).
//
// Dibujado a mano en SVG y no con una libreria de graficos: son catorce barras con su etiqueta.
// Una libreria resolveria esto, pero agregaria varios cientos de kilobytes al sistema y una
// dependencia mas que mantener, para algo que son treinta lineas.
const ALTO = 150
const ANCHO_BARRA = 28
const SEPARACION = 8

// "2026-10-23" al nombre corto del dia y su numero. Se arma con las partes de la fecha y no con
// new Date(texto), que interpreta la fecha en UTC y en Argentina corre el dia.
const etiqueta = (iso) => {
  const [anio, mes, dia] = iso.split('-').map(Number)
  const fecha = new Date(anio, mes - 1, dia)
  return { nombre: fecha.toLocaleDateString('es-AR', { weekday: 'short' }).slice(0, 3), numero: dia }
}

export default function SalesChart({ days }) {
  if (days.length === 0) {
    return null
  }

  // El maximo marca la escala. El uno evita dividir por cero cuando no se vendio nada en toda
  // la quincena, que al abrir el sistema por primera vez es lo normal.
  const maximo = Math.max(...days.map((day) => Number(day.total)), 1)
  const ancho = days.length * (ANCHO_BARRA + SEPARACION)

  return (
    <div className="chart">
      <svg viewBox={`0 0 ${ancho} ${ALTO + 36}`} className="chart-svg" role="img"
        aria-label="Ventas de los últimos días">
        {days.map((day, i) => {
          const valor = Number(day.total)
          const alto = Math.round((valor / maximo) * ALTO)
          const x = i * (ANCHO_BARRA + SEPARACION)
          const { nombre, numero } = etiqueta(day.date)
          return (
            <g key={day.date}>
              {/* El importe exacto al pasar el mouse: el grafico muestra la forma, no los numeros. */}
              <title>{`${numero}/${day.date.slice(5, 7)}: ${formatPrice(valor)}`}</title>
              <rect
                x={x}
                y={ALTO - alto}
                width={ANCHO_BARRA}
                height={Math.max(alto, 2)}
                rx="3"
                className={valor === 0 ? 'chart-bar chart-bar-empty' : 'chart-bar'}
              />
              <text x={x + ANCHO_BARRA / 2} y={ALTO + 15} className="chart-day">{numero}</text>
              <text x={x + ANCHO_BARRA / 2} y={ALTO + 29} className="chart-weekday">{nombre}</text>
            </g>
          )
        })}
      </svg>
      <p className="chart-scale">El día más alto del período: {formatPrice(maximo)}</p>
    </div>
  )
}
