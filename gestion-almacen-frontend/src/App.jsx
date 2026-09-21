import { useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { getSessionUser, logout } from './api/auth'
import Layout from './components/Layout'
import Home from './pages/Home'
import Login from './pages/Login'
import Products from './pages/Products'
import Stock from './pages/Stock'
import Suppliers from './pages/Suppliers'
import Users from './pages/Users'

// Rutas de la aplicacion. Sin sesion se muestra el inicio de sesion (CU-01).
export default function App() {
  const [user, setUser] = useState(null)
  const [checking, setChecking] = useState(true)

  // Al abrir: si la cookie de sesion sigue siendo valida, se entra directo.
  useEffect(() => {
    getSessionUser()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setChecking(false))
  }, [])

  const onLogout = async () => {
    await logout().catch(() => {}) // aunque falle el servidor, en el navegador la sesion se cierra igual
    setUser(null)
  }

  if (checking) return <p className="notice">Cargando...</p>

  if (!user) return <Login onLogin={setUser} />

  const isAdmin = user.role === 'ADMIN'

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout user={user} onLogout={onLogout} />}>
          <Route index element={<Home user={user} />} />
          <Route path="products" element={<Products isAdmin={isAdmin} />} />
          <Route path="stock" element={<Stock />} />
          <Route path="suppliers" element={<Suppliers />} />
          {/* RF-11: la gestion de usuarios es solo del administrador. */}
          {isAdmin && <Route path="users" element={<Users />} />}
          {/* Cualquier otra direccion vuelve al inicio, incluida /users para un empleado. */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
