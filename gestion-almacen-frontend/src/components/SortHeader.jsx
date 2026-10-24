// Encabezado de columna que ordena el listado al hacer clic (RF-13).
//
// column es el valor que espera el backend: NOMBRE, PRECIO, STOCK o CODIGO. La flecha marca por
// cual se esta ordenando y en que sentido, que es lo que evita tener que adivinarlo.
export default function SortHeader({ column, sort, direction, onSort, className, children }) {
  const activa = sort === column
  return (
    <th className={className}>
      <button type="button" className={activa ? 'sort sort-active' : 'sort'} onClick={() => onSort(column)}>
        {children}
        {activa && <span className="sort-arrow">{direction === 'ASC' ? '↑' : '↓'}</span>}
      </button>
    </th>
  )
}
