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

// Le prix d'une location est un loyer : il s'affiche « par mois »
export const formatPrixBien = (bien, t) =>
  bien.typeOffre === 'location' ? t('bien.loyerParMois', { prix: formatPrix(bien.prix) }) : formatPrix(bien.prix)

// Chaque type d'offre a sa page de résultats
export const TYPES_OFFRE = ['vente', 'location']
export const cheminListe = (typeOffre) => ({ vente: '/a-vendre', location: '/a-louer' })[typeOffre] ?? '/biens'
