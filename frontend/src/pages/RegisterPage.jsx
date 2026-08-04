import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { register } from '../api/auth.js'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', password: '' })
  const [err, setErr] = useState(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setErr(null)
    if (form.password.length < 8) {
      setErr('Password must be at least 8 characters')
      return
    }
    setLoading(true)
    try {
      await register(form.username.trim(), form.password)
      navigate('/login')
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
        <h2>Create account</h2>
        {err && <p className="error">{err}</p>}
        <input
          type="text"
          placeholder="Username (3–50 chars)"
          autoComplete="username"
          minLength={3}
          maxLength={50}
          value={form.username}
          onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
          required
        />
        <input
          type="password"
          placeholder="Password (min 8 chars)"
          autoComplete="new-password"
          value={form.password}
          onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
          required
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Creating…' : 'Create account'}
        </button>
        <p className="auth-link">Already have an account? <Link to="/login">Sign in</Link></p>
      </form>
    </div>
  )
}
