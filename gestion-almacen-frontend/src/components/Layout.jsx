import { NavLink, Outlet } from 'react-router-dom'

// Estructura comun a todas las pantallas: menu a la izquierda y contenido a la derecha.
export default function Layout({ user, onLogout }) {
  // NavLink marca el enlace de la seccion en la que se esta.
  const linkClass = ({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')
  const isAdmin = user.role === 'ADMIN'

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
          <NavLink to="/sale" className={linkClass}>Nueva venta</NavLink>
          {/* RF-07: el historial es del administrador. end: el ticket de una venta nueva no marca esta opcion. */}
          {isAdmin && <NavLink to="/sales" className={linkClass} end>Historial de ventas</NavLink>}
          <NavLink to="/products" className={linkClass}>Productos</NavLink>
          <NavLink to="/stock" className={linkClass}>Stock</NavLink>
          {/* RF-10: las ofertas las decide el administrador. */}
          {isAdmin && <NavLink to="/offers" className={linkClass}>Ofertas</NavLink>}
          <NavLink to="/suppliers" className={linkClass}>Proveedores</NavLink>
          {/* El empleado no gestiona usuarios (RF-11), asi que ni ve la opcion. Igual quien lo controla es el backend. */}
          {isAdmin && <NavLink to="/users" className={linkClass}>Usuarios</NavLink>}
        </nav>

        <div className="session">
          <strong>{user.name} {user.lastName}</strong>
          <small>{isAdmin ? 'Administrador' : 'Empleado'}</small>
          <button type="button" className="button button-secondary button-wide" onClick={onLogout}>
            Cerrar sesión
          </button>
        </div>
      </aside>

      {/* Outlet es donde React Router dibuja la pantalla de la ruta actual. */}
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
