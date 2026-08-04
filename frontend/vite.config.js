import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const api = env.VITE_API_URL || 'http://localhost:8080'

  return {
    plugins: [react()],
    // GitHub Pages serves from /books-api/ — must match the repo name
    base: process.env.GITHUB_ACTIONS ? '/books-api/' : '/',
    server: {
      port: 3000,
      proxy: {
        '/auth':  { target: api, changeOrigin: true },
        '/books': { target: api, changeOrigin: true },
      },
    },
  }
})
