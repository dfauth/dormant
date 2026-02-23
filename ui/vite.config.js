import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': 'http://localhost:8081',
      '/oauth2': 'http://localhost:8081',
      '/login': 'http://localhost:8081',
      '/logout': 'http://localhost:8081',
    },
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
