import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // Frontend code calls relative paths (/api/...) rather than a hardcoded backend origin -
      // works unchanged in dev (proxied here) and in any deployment where the backend sits
      // behind the same origin/reverse proxy as the built frontend.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
