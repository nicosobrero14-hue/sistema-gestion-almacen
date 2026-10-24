// Controles de pagina de un listado (RF-13).
//
// El total se muestra siempre, porque saber cuantos hay sirve aunque entren en una sola pagina.
// Los botones aparecen recien cuando hay mas de una: antes no harian nada.
// name: el nombre de lo que se lista, en singular. Por ejemplo "producto".
export default function Pagination({ page, totalPages, totalElements, name, onChange }) {
  return (
    <div className="pagination">
      <span className="pagination-total">
        {totalElements} {totalElements === 1 ? name : name + 's'}
      </span>

      {totalPages > 1 && (
        <div className="pagination-pages">
          <button type="button" className="button button-small button-secondary"
            disabled={page === 0} onClick={() => onChange(page - 1)}>
            Anterior
          </button>
          <span className="pagination-position">Página {page + 1} de {totalPages}</span>
          <button type="button" className="button button-small button-secondary"
            disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
            Siguiente
          </button>
        </div>
      )}
    </div>
  )
}
