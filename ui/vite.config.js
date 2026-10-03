import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    proxy: {
      '/api': 'http://dormant-container:8081',
      '/oauth2': 'http://dormant-container:8081',
      '/login': 'http://dormant-container:8081',
      '/logout': 'http://dormant-container:8081',
    },
  },
  preview: {
    host: '0.0.0.0',
    port: 4173,
    strictPort: true,
    proxy: {
      // Directs frontend /api requests to the backend container internally
      '/api': {
        target: 'http://dormant-container:8081',
        changeOrigin: true,
        secure: false,
      },
      '/oauth2': {
        target: 'http://dormant-container:8081',
        changeOrigin: true,
        secure: false,
      },
      '/login': {
        target: 'http://dormant-container:8081',
        changeOrigin: true,
        secure: false,
      },
      '/logout': {
        target: 'http://dormant-container:8081',
        changeOrigin: true,
        secure: false,
      },
    }
  },
  test: {
    environment: 'jsdom',
    environmentOptions: { url: 'http://localhost' },
    setupFiles: ['./src/test/setup.js'],
    globals: true,
    css: false,
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{js,jsx}'],
      exclude: ['src/test/**'],
      reporter: ['text', 'lcov'],
    },
  },
})
