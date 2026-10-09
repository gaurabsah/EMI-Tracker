import { Navigate, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth'

const NAV = {
  ADMIN: [{ to: '/admin/companies', label: 'Companies' }],
  COMPANY: [
    { to: '/company/purchases', label: 'Purchases' },
    { to: '/company/plans', label: 'Plans' },
    { to: '/company/customers', label: 'Customers' },
  ],
  CUSTOMER: [{ to: '/me/purchases', label: 'My purchases' }],
}

export default function Layout() {
  const { auth, logout } = useAuth()
  if (!auth) return <Navigate to="/login" replace />
  return (
    <>
      <header className="topbar">
        <strong className="brand">EMI Tracker</strong>
        <nav>
          {NAV[auth.role].map((item) => (
            <NavLink key={item.to} to={item.to}>{item.label}</NavLink>
          ))}
        </nav>
        <span className="spacer" />
        <span className="muted">{auth.email} ({auth.role.toLowerCase()})</span>
        <button className="link" onClick={logout}>Log out</button>
      </header>
      <main>
        <Outlet />
      </main>
    </>
  )
}
