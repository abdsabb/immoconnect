import { useQuery } from '@tanstack/react-query'
import { api } from './api'

// Réglages publics du site (GET /configuration) : options de sécurité, boîte de démonstration,
// identité de l'agence et langues actives réglées par l'administrateur (cas A5). Lus une fois.
export const chargerConfiguration = () => api.get('/configuration').then((r) => r.data)

export const useConfiguration = () =>
  useQuery({ queryKey: ['configuration'], queryFn: chargerConfiguration, staleTime: Infinity })

export const exporterMesDonnees = () => api.get('/auth/me/export', { responseType: 'blob' }).then((r) => r.data)

export const signaler = (signalement) => api.post('/signalements', signalement).then((r) => r.data)
