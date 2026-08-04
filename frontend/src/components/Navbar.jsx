import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Navbar() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()

  function handleSignOut() {
    signOut()
    navigate('/login')
  }

  return (
    <nav className="navbar">
      <span className="nav-brand">Books API</span>
      <div className="nav-right">
        <span className="nav-user">
          {user?.username}
          <span className="role-badge">{user?.role}</span>
        </span>
        <button className="btn-outline" onClick={handleSignOut}>Sign out</button>
      </div>
    </nav>
  )
}
