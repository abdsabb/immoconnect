/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // En dev, les appels /api et les photos (/storage) sont relayés vers le backend Spring Boot
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/storage': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  // Tests des composants (Vitest + React Testing Library) : DOM simulé par jsdom, textes en français
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/tests/installation.js'],
    css: false,
  },
})
