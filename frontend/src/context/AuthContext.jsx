import { createContext, useContext, useState } from 'react'
import { setToken, clearToken } from '../api/client.js'

const Ctx = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)

  function signIn(data) {
    setToken(data.access_token)
    sessionStorage.setItem('refresh_token', data.refresh_token)
    setUser({ username: data.username, role: data.role })
  }

  function signOut() {
    clearToken()
    sessionStorage.removeItem('refresh_token')
    setUser(null)
  }

  return <Ctx.Provider value={{ user, signIn, signOut }}>{children}</Ctx.Provider>
}

export const useAuth = () => useContext(Ctx)
