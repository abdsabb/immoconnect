import axios from 'axios'

// Client HTTP unique vers l'API REST ImmoConnect (/api/v1, livrable 15).
// Le jeton JWT sera injecté ici en mémoire (jamais en localStorage — livrable 16).
export const api = axios.create({
  baseURL: '/api/v1',
  headers: { Accept: 'application/json' },
})
