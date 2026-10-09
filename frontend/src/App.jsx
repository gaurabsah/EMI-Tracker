import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, HOME, useAuth } from './auth'
import Layout from './components/Layout'
import Login from './pages/Login'
import Companies from './pages/admin/Companies'
import CompanyPurchaseDetail from './pages/company/CompanyPurchaseDetail'
import Customers from './pages/company/Customers'
import Plans from './pages/company/Plans'
import Purchases from './pages/company/Purchases'
import MyPurchaseDetail from './pages/customer/MyPurchaseDetail'
import MyPurchases from './pages/customer/MyPurchases'

function RequireRole({ role, children }) {
  const { auth } = useAuth()
  if (!auth) return <Navigate to="/login" replace />
  if (auth.role !== role) return <Navigate to={HOME[auth.role]} replace />
  return children
}

function Home() {
  const { auth } = useAuth()
  return <Navigate to={auth ? HOME[auth.role] : '/login'} replace />
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<Layout />}>
            <Route path="/admin/companies" element={<RequireRole role="ADMIN"><Companies /></RequireRole>} />
            <Route path="/company/plans" element={<RequireRole role="COMPANY"><Plans /></RequireRole>} />
            <Route path="/company/customers" element={<RequireRole role="COMPANY"><Customers /></RequireRole>} />
            <Route path="/company/purchases" element={<RequireRole role="COMPANY"><Purchases /></RequireRole>} />
            <Route path="/company/purchases/:id" element={<RequireRole role="COMPANY"><CompanyPurchaseDetail /></RequireRole>} />
            <Route path="/me/purchases" element={<RequireRole role="CUSTOMER"><MyPurchases /></RequireRole>} />
            <Route path="/me/purchases/:id" element={<RequireRole role="CUSTOMER"><MyPurchaseDetail /></RequireRole>} />
          </Route>
          <Route path="*" element={<Home />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
