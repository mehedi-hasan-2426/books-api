import { useState } from 'react'

export default function AddBookForm({ onSubmit }) {
  const [form, setForm] = useState({ title: '', author: '', genre: '' })
  const [err, setErr] = useState(null)
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setErr(null)
    setSuccess(false)
    setLoading(true)
    try {
      await onSubmit({
        title: form.title.trim(),
        author: form.author.trim(),
        genre: form.genre.trim() || undefined,
      })
      setForm({ title: '', author: '', genre: '' })
      setSuccess(true)
      setTimeout(() => setSuccess(false), 3000)
    } catch (e) {
      setErr(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="add-form" onSubmit={handleSubmit}>
      <h2>Add book</h2>
      {err && <p className="error">{err}</p>}
      {success && <p className="success">Book added!</p>}
      <div className="form-row">
        <input
          placeholder="Title *"
          value={form.title}
          onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
          required
        />
        <input
          placeholder="Author *"
          value={form.author}
          onChange={e => setForm(f => ({ ...f, author: e.target.value }))}
          required
        />
        <input
          placeholder="Genre (optional)"
          value={form.genre}
          onChange={e => setForm(f => ({ ...f, genre: e.target.value }))}
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Adding…' : 'Add'}
        </button>
      </div>
    </form>
  )
}
