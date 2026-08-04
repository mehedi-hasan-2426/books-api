import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { listBooks, createBook, deleteBook } from '../api/books.js'
import { useAuth } from '../context/AuthContext.jsx'
import Navbar from '../components/Navbar.jsx'
import BookCard from '../components/BookCard.jsx'
import AddBookForm from '../components/AddBookForm.jsx'

export default function BooksPage() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()

  const [books, setBooks] = useState([])
  const [cursor, setCursor] = useState(null)
  const [hasMore, setHasMore] = useState(false)
  const [search, setSearch] = useState('')
  const [err, setErr] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async (q, cur) => {
    setLoading(true)
    setErr(null)
    try {
      const data = await listBooks({ cursor: cur, q })
      setBooks(prev => cur ? [...prev, ...data.items] : data.items)
      setCursor(data.next ?? null)
      setHasMore(!!data.next)
    } catch (e) {
      if (e.status === 401) { signOut(); navigate('/login'); return }
      setErr(e.message)
    } finally {
      setLoading(false)
    }
  }, [signOut, navigate])

  useEffect(() => { load('', null) }, []) // eslint-disable-line

  function handleSearch(e) {
    e.preventDefault()
    load(search, null)
  }

  async function handleCreate(data) {
    const book = await createBook(data)
    setBooks(prev => [book, ...prev])
  }

  async function handleDelete(id) {
    await deleteBook(id)
    setBooks(prev => prev.filter(b => b.id !== id))
  }

  return (
    <div className="page">
      <Navbar />
      <main className="container">
        <div className="toolbar">
          <form onSubmit={handleSearch} className="search-form">
            <input
              type="search"
              placeholder="Search by title or author…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
            <button type="submit">Search</button>
          </form>
        </div>

        {err && <p className="error">{err}</p>}

        <AddBookForm onSubmit={handleCreate} />

        <div className="book-grid">
          {books.map(b => (
            <BookCard
              key={b.id}
              book={b}
              canDelete={user?.role === 'ADMIN'}
              onDelete={handleDelete}
            />
          ))}
          {books.length === 0 && !loading && <p className="empty">No books yet — add one above.</p>}
        </div>

        {hasMore && (
          <button className="load-more" onClick={() => load(search, cursor)} disabled={loading}>
            {loading ? 'Loading…' : 'Load more'}
          </button>
        )}
      </main>
    </div>
  )
}
