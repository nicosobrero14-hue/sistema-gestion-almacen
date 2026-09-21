import { NavLink, Outlet } from 'react-router-dom'

// Estructura comun a todas las pantallas: menu a la izquierda y contenido a la derecha.
export default function Layout() {
  // NavLink marca el enlace de la seccion en la que se esta.
  const linkClass = ({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-icon" aria-hidden="true">📦</span>
          <div>
            <strong>Gestión de Almacén</strong>
            <small>Control de stock</small>
          </div>
        </div>

        <nav aria-label="Menú principal">
          <NavLink to="/" className={linkClass} end>Inicio</NavLink>
          <NavLink to="/products" className={linkClass}>Productos</NavLink>
          <NavLink to="/suppliers" className={linkClass}>Proveedores</NavLink>
          <NavLink to="/users" className={linkClass}>Usuarios</NavLink>
        </nav>
      </aside>

      {/* Outlet es donde React Router dibuja la pantalla de la ruta actual. */}
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
