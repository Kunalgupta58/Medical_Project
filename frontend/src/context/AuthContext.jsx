import { createContext, useContext, useState, useCallback } from 'react'
import { authApi } from '../api/auth'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('ec360_user')) } catch { return null }
  })
  const [loading, setLoading] = useState(false)

  const login = useCallback(async (email, password) => {
    setLoading(true)
    try {
      const { data } = await authApi.login({ email, password })
      localStorage.setItem('ec360_token', data.accessToken)
      localStorage.setItem('ec360_user', JSON.stringify(data))
      setUser(data)
      return data
    } finally {
      setLoading(false)
    }
  }, [])

  const register = useCallback(async (formData) => {
    setLoading(true)
    try {
      const { data } = await authApi.register(formData)
      localStorage.setItem('ec360_token', data.accessToken)
      localStorage.setItem('ec360_user', JSON.stringify(data))
      setUser(data)
      return data
    } finally {
      setLoading(false)
    }
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('ec360_token')
    localStorage.removeItem('ec360_user')
    setUser(null)
  }, [])

  const isElderly      = user?.role === 'ELDERLY'
  const isFamilyMember = user?.role === 'FAMILY_MEMBER'
  const isCaregiver    = user?.role === 'CAREGIVER'
  const isAdmin        = user?.role === 'ADMIN'

  return (
    <AuthContext.Provider value={{ user, login, register, logout, loading, isElderly, isFamilyMember, isCaregiver, isAdmin }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
