/// <reference types="vitest/config" />
import { defineConfig } from 'vitest/config'
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
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    globals: true,
    // Without this, a vi.fn()'s call history from one test leaks into the next test's
    // assertions within the same file - it's not just a "cleaner" default here, tests that
    // assert "not called yet" (e.g. DocumentsPage's confirm-before-upload flow) are silently
    // wrong without it.
    mockReset: true,
  },
})
