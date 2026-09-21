import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // El navegador le pide /api a Vite (5173) y Vite se lo pasa al backend (8080).
    // Asi todo sale del mismo sitio y no hace falta configurar CORS.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
