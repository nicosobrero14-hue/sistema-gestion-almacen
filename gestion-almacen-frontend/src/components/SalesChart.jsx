import { useState } from 'react'
import { formatPrice } from '../utils/format'

// Grafico de lo vendido en los ultimos dias (RF-15).
//
// Dibujado a mano en SVG y no con una libreria de graficos. Una libreria de graficos pesa unos
// cuantos cientos de kilobytes y trae ejes, leyendas, escalas y animaciones que aca no se usan:
// esto son catorce barras. Si mas adelante el sistema necesita varios graficos distintos, ahi si
// conviene revisarlo.
const ALTO = 150
const ANCHO_BARRA = 26
const SEPARACION = 10
const TECHO = 24 // lugar arriba para el importe que aparece al pasar el mouse
const PIE = 40 // lugar abajo para el dia y su nombre

// "2026-10-23" al nombre corto del dia y su numero. Se arma con las partes de la fecha y no con
// new Date(texto), que interpreta la fecha en UTC y en Argentina corre el dia.
const etiqueta = (iso) => {
  const [anio, mes, dia] = iso.split('-').map(Number)
  const fecha = new Date(anio, mes - 1, dia)
  return {
    nombre: fecha.toLocaleDateString('es-AR', { weekday: 'short' }).slice(0, 3),
    numero: dia,
    finDeSemana: fecha.getDay() === 0 || fecha.getDay() === 6,
  }
}

export default function SalesChart({ days }) {
  // La barra sobre la que esta el mouse. null cuando no hay ninguna.
  const [activa, setActiva] = useState(null)

  if (days.length === 0) {
    return null
  }

  // El maximo marca la escala. El uno evita dividir por cero cuando no se vendio nada en toda
  // la quincena, que al abrir el sistema por primera vez es lo normal.
  const maximo = Math.max(...days.map((day) => Number(day.total)), 1)
  const paso = ANCHO_BARRA + SEPARACION
  const ancho = days.length * paso - SEPARACION
  const alto = TECHO + ALTO + PIE

  return (
    <div className="chart">
      <svg viewBox={`0 0 ${ancho} ${alto}`} className="chart-svg" role="img"
        aria-label={`Ventas de los últimos ${days.length} días`}>
        <defs>
          {/* El degradado da volumen: una barra de un color plano es lo que hace que un
              grafico se vea sin terminar. */}
          <linearGradient id="barraVenta" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#52a558" />
            <stop offset="100%" stopColor="#2e7d32" />
          </linearGradient>
          <linearGradient id="barraHoy" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#f0b429" />
            <stop offset="100%" stopColor="#d99409" />
          </linearGradient>
        </defs>

        {/* Lineas de referencia: la de abajo es el cero y las otras dos ayudan a estimar
            cuanto vale una barra sin tener que pasar el mouse. */}
        {[0, 0.5, 1].map((parte) => (
          <line
            key={parte}
            x1="0"
            x2={ancho}
            y1={TECHO + ALTO - parte * ALTO}
            y2={TECHO + ALTO - parte * ALTO}
            className={parte === 0 ? 'chart-axis' : 'chart-grid'}
          />
        ))}

        {days.map((day, i) => {
          const valor = Number(day.total)
          const altoBarra = valor > 0 ? Math.max(Math.round((valor / maximo) * ALTO), 4) : 3
          const x = i * paso
          const y = TECHO + ALTO - altoBarra
          const hoy = i === days.length - 1
          const sobre = activa === i
          const { nombre, numero, finDeSemana } = etiqueta(day.date)

          let clase = 'chart-bar'
          if (valor === 0) clase = 'chart-bar chart-bar-empty'
          else if (hoy) clase = 'chart-bar chart-bar-today'
          if (activa !== null && !sobre) clase += ' chart-bar-dim'

          return (
            <g
              key={day.date}
              onMouseEnter={() => setActiva(i)}
              onMouseLeave={() => setActiva(null)}
            >
              <title>{`${numero}/${day.date.slice(5, 7)}: ${formatPrice(valor)}`}</title>

              {/* Zona sensible de toda la columna: asi el importe aparece al acercarse,
                  sin tener que apuntarle justo a una barra de tres pixeles. */}
              <rect x={x} y={TECHO} width={ANCHO_BARRA} height={ALTO} className="chart-hit" />

              <rect x={x} y={y} width={ANCHO_BARRA} height={altoBarra} rx="4" className={clase} />

              {sobre && (
                <text x={x + ANCHO_BARRA / 2} y={y - 8} className="chart-amount">
                  {formatPrice(valor)}
                </text>
              )}

              <text
                x={x + ANCHO_BARRA / 2}
                y={TECHO + ALTO + 17}
                className={hoy ? 'chart-day chart-day-today' : 'chart-day'}
              >
                {numero}
              </text>
              <text
                x={x + ANCHO_BARRA / 2}
                y={TECHO + ALTO + 31}
                className={finDeSemana ? 'chart-weekday chart-weekend' : 'chart-weekday'}
              >
                {nombre}
              </text>
            </g>
          )
        })}
      </svg>

      <p className="chart-scale">
        <span className="chart-key chart-key-today" aria-hidden="true" /> Hoy
        <span className="chart-separator">·</span>
        El día más alto del período: <strong>{formatPrice(maximo)}</strong>
      </p>
    </div>
  )
}
