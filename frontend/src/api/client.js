// Access token lives in memory only — never in localStorage (XSS defence)
let token = null

export function setToken(t) { token = t }
export function clearToken() { token = null }

async function tryRefresh() {
  const rt = sessionStorage.getItem('refresh_token')
  if (!rt) return false
  const res = await fetch('/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refresh_token: rt }),
  })
  if (!res.ok) {
    sessionStorage.removeItem('refresh_token')
    return false
  }
  const data = await res.json()
  token = data.access_token
  sessionStorage.setItem('refresh_token', data.refresh_token)
  return true
}

export async function http(path, opts = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...opts.headers,
  }

  let res = await fetch(path, { ...opts, headers })

  if (res.status === 401) {
    const ok = await tryRefresh()
    if (!ok) throw Object.assign(new Error('Session expired'), { status: 401 })
    res = await fetch(path, {
      ...opts,
      headers: { ...headers, Authorization: `Bearer ${token}` },
    })
  }

  return res
}
