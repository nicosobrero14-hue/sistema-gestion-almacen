import { useEffect, useState } from 'react'
import { activateSupplier, deactivateSupplier, getSuppliers } from '../api/suppliers'
import Message from '../components/Message'
import SupplierForm from './SupplierForm'

// Pantalla de proveedores (RF-08 / CU-05).
export default function Suppliers() {
  const [suppliers, setSuppliers] = useState([])
  const [search, setSearch] = useState('')
  const [activeOnly, setActiveOnly] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Formulario: abierto o cerrado, y el proveedor que se edita (null = proveedor nuevo).
  const [formOpen, setFormOpen] = useState(false)
  const [selected, setSelected] = useState(null)

  const load = () => {
    getSuppliers(search, activeOnly)
      .then(setSuppliers)
      .catch((e) => setError(e.message))
  }

  // Vuelve a buscar cuando cambia el texto o la casilla. Espera 300 ms a que se termine de escribir.
  useEffect(() => {
    const timer = setTimeout(load, 300)
    return () => clearTimeout(timer)
  }, [search, activeOnly])

  const openForm = (supplier) => {
    setSelected(supplier)
    setFormOpen(true)
  }

  // La llama el formulario cuando guarda bien.
  const onSaved = (message) => {
    setFormOpen(false)
    setSuccess(message)
    load()
  }

  const toggleActive = async (supplier) => {
    try {
      if (supplier.active) {
        if (!window.confirm(`¿Dar de baja a "${supplier.name}"? Sus productos siguen en el catálogo.`)) return
        await deactivateSupplier(supplier.id)
        setSuccess(`"${supplier.name}" fue dado de baja.`)
      } else {
        await activateSupplier(supplier.id)
        setSuccess(`"${supplier.name}" fue reactivado.`)
      }
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Proveedores</h1>
          <p className="subtitle">Proveedores que abastecen al comercio.</p>
        </div>
        <button type="button" className="button button-primary" onClick={() => openForm(null)}>
          + Nuevo proveedor
        </button>
      </div>

      <div className="filters">
        <input
          type="search"
          placeholder="Buscar por nombre o apellido..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          aria-label="Buscar proveedores"
        />
        <label className="checkbox">
          <input type="checkbox" checked={activeOnly} onChange={(event) => setActiveOnly(event.target.checked)} />
          Ver solo activos
        </label>
      </div>

      <Message type="error" text={error} onClose={() => setError('')} />
      <Message type="success" text={success} onClose={() => setSuccess('')} />

      {suppliers.length === 0 ? (
        <p className="notice">No se encontraron proveedores.</p>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Proveedor</th>
                <th>Email</th>
                <th>Teléfono</th>
                <th>Dirección</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {suppliers.map((supplier) => (
                <tr key={supplier.id} className={supplier.active ? '' : 'inactive-row'}>
                  <td>{supplier.name} {supplier.lastName}</td>
                  <td>{supplier.email ?? '—'}</td>
                  <td>{supplier.phone ?? '—'}</td>
                  <td>{supplier.address ?? '—'}</td>
                  <td>
                    <span className={supplier.active ? 'chip chip-active' : 'chip chip-inactive'}>
                      {supplier.active ? 'Activo' : 'Dado de baja'}
                    </span>
                  </td>
                  <td className="actions">
                    <button type="button" className="button button-small" onClick={() => openForm(supplier)}>
                      Editar
                    </button>
                    <button type="button" className="button button-small button-secondary" onClick={() => toggleActive(supplier)}>
                      {supplier.active ? 'Dar de baja' : 'Reactivar'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {formOpen && <SupplierForm supplier={selected} onSaved={onSaved} onCancel={() => setFormOpen(false)} />}
    </>
  )
}
