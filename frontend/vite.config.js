import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // En dev, les appels /api sont relayés vers le backend Spring Boot
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } },
  },
})
