import { api } from './api'

// Cas V1/V2 : GET /api/v1/biens — recherche multicritères paginée (livrable 15, §6.2)
export async function rechercherBiens(criteres = {}) {
  const params = Object.fromEntries(
    Object.entries(criteres).filter(([, v]) => v !== '' && v !== null && v !== undefined),
  )
  const { data } = await api.get('/biens', { params })
  return data
}

// Cas V4 : GET /api/v1/biens/{id}
export async function chargerBien(id) {
  const { data } = await api.get(`/biens/${id}`)
  return data
}

export const formatPrix = (prix) =>
  new Intl.NumberFormat('fr-BE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(prix)
