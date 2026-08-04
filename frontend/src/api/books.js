import { http } from './client.js'

export async function listBooks({ cursor, q } = {}) {
  const p = new URLSearchParams({ limit: '20' })
  if (cursor) p.set('cursor', cursor)
  if (q?.trim()) p.set('q', q.trim())
  const res = await http(`/books?${p}`)
  if (!res.ok) throw new Error('Failed to load books')
  return res.json()
}

export async function createBook(data) {
  const res = await http('/books', {
    method: 'POST',
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.detail ?? 'Failed to create book')
  }
  return res.json()
}

export async function deleteBook(id) {
  const res = await http(`/books/${id}`, { method: 'DELETE' })
  if (res.status !== 204 && !res.ok) {
    const err = await res.json()
    throw new Error(err.detail ?? 'Failed to delete book')
  }
}
