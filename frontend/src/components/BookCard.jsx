import { useState } from 'react'

export default function BookCard({ book, canDelete, onDelete }) {
  const [deleting, setDeleting] = useState(false)
  const [err, setErr] = useState(null)

  async function handleDelete() {
    if (!confirm(`Delete "${book.title}"?`)) return
    setDeleting(true)
    try {
      await onDelete(book.id)
    } catch (e) {
      setErr(e.message)
      setDeleting(false)
    }
  }

  return (
    <div className="book-card">
      <div className="book-body">
        <h3>{book.title}</h3>
        <p className="author">{book.author}</p>
        {book.genre && <span className="genre">{book.genre}</span>}
      </div>
      {err && <p className="error small">{err}</p>}
      {canDelete && (
        <button className="btn-danger" onClick={handleDelete} disabled={deleting}>
          {deleting ? 'Deleting…' : 'Delete'}
        </button>
      )}
    </div>
  )
}
