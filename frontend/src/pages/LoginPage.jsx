import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { login } from '../api/auth.js'
import { useAuth } from '../context/AuthContext.jsx'

export default function LoginPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', password: '' })
  const [err, setErr] = useState(null)
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setErr(null)
    setLoading(true)
    try {
      const data = await login(form.username.trim(), form.password)
      signIn(data)
      navigate('/books')
    } catch (e) {
      setErr(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <h1>Books API</h1>
        <h2>Sign in</h2>
        {err && <p className="error">{err}</p>}
        <input
          type="text"
          placeholder="Username"
          autoComplete="username"
          value={form.username}
          onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
          required
        />
        <div className="password-wrapper">
          <input
            type={showPassword ? 'text' : 'password'}
            placeholder="Password"
            autoComplete="current-password"
            value={form.password}
            onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
            required
          />
          <button type="button" className="show-password" onClick={() => setShowPassword(v => !v)} aria-label="Toggle password visibility">
            {showPassword ? '🙈' : '👁️'}
          </button>
        </div>
        <button type="submit" disabled={loading}>
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
        <p className="auth-link">No account? <Link to="/register">Register</Link></p>
      </form>
    </div>
  )
}
