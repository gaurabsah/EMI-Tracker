import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, loadAuth, saveAuth, setUnauthorizedHandler } from './api'

export const HOME = {
  ADMIN: '/admin/companies',
  COMPANY: '/company/purchases',
  CUSTOMER: '/me/purchases',
}

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(loadAuth)

  const login = useCallback(async (email, password) => {
    const { token, role } = await api.login(email, password)
    const next = { token, role, email }
    saveAuth(next)
    setAuth(next)
    return next
  }, [])

  const logout = useCallback(() => {
    saveAuth(null)
    setAuth(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
  }, [logout])

  const value = useMemo(() => ({ auth, login, logout }), [auth, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
