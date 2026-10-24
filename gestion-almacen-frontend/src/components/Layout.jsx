import {
  Boxes, FileText, History, LayoutDashboard, LogOut, Package, ShoppingCart, Tag, Truck, Users,
} from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import logo from '../assets/logo.png'

// Las iniciales para el circulo del pie del menu: "Nicolás Sobrero" queda en NS.
const initials = (user) => `${user.name?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()

// Estructura comun a todas las pantallas: menu a la izquierda y contenido a la derecha.
//
// Cada opcion lleva su icono. No es decoracion: en una pantalla de mostrador la opcion se
// busca de reojo y con las manos ocupadas, y una forma se reconoce mas rapido que una palabra.
export default function Layout({ user, onLogout }) {
  // NavLink marca el enlace de la seccion en la que se esta.
  const linkClass = ({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')
  const isAdmin = user.role === 'ADMIN'

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">
          <img src={logo} alt="Logo del Sistema de Gestión de Almacén" className="brand-logo" />
          <strong>Gestión de Almacén</strong>
          <small>Control de stock</small>
        </div>

        <nav aria-label="Menú principal">
          <NavLink to="/" className={linkClass} end>
            <LayoutDashboard aria-hidden="true" /> Inicio
          </NavLink>
          <NavLink to="/sale" className={linkClass}>
            <ShoppingCart aria-hidden="true" /> Nueva venta
          </NavLink>
          {/* RF-07: el historial es del administrador. end: el ticket de una venta nueva no marca esta opcion. */}
          {isAdmin && (
            <NavLink to="/sales" className={linkClass} end>
              <History aria-hidden="true" /> Historial de ventas
            </NavLink>
          )}
          <NavLink to="/products" className={linkClass}>
            <Package aria-hidden="true" /> Productos
          </NavLink>
          <NavLink to="/stock" className={linkClass}>
            <Boxes aria-hidden="true" /> Stock
          </NavLink>
          {/* RF-10: las ofertas las decide el administrador. */}
          {isAdmin && (
            <NavLink to="/offers" className={linkClass}>
              <Tag aria-hidden="true" /> Ofertas
            </NavLink>
          )}
          <NavLink to="/suppliers" className={linkClass}>
            <Truck aria-hidden="true" /> Proveedores
          </NavLink>
          {/* RF-15: las compras muestran saldos, asi que son del administrador. */}
          {isAdmin && (
            <NavLink to="/purchases" className={linkClass}>
              <FileText aria-hidden="true" /> Compras
            </NavLink>
          )}
          {/* El empleado no gestiona usuarios (RF-11), asi que ni ve la opcion. Igual quien lo controla es el backend. */}
          {isAdmin && (
            <NavLink to="/users" className={linkClass}>
              <Users aria-hidden="true" /> Usuarios
            </NavLink>
          )}
        </nav>

        <div className="session">
          <div className="session-user">
            <span className="avatar" aria-hidden="true">{initials(user)}</span>
            <div>
              <strong>{user.name} {user.lastName}</strong>
              <small>{isAdmin ? 'Administrador' : 'Empleado'}</small>
            </div>
          </div>
          <button type="button" className="button button-secondary button-wide" onClick={onLogout}>
            <LogOut aria-hidden="true" /> Cerrar sesión
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
