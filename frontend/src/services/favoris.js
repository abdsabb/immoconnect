import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'
import { useAuth } from '../auth/AuthContext'

// Cas M1 / M2 : favoris du membre connecté (livrable 15, §6.3)

export async function chargerFavoris() {
  const { data } = await api.get('/membres/moi/favoris', { params: { taille: 100 } })
  return data
}

/**
 * Favoris du membre connecté : la liste, l'appartenance d'un bien et la bascule ajout/retrait.
 * Pour un visiteur ou un agent, la liste est vide et rien n'est demandé à l'API.
 */
export function useFavoris() {
  const { utilisateur } = useAuth()
  const estMembre = utilisateur?.role === 'membre'
  const queryClient = useQueryClient()
  const cle = ['favoris', utilisateur?.id]

  const { data, isPending, isError } = useQuery({ queryKey: cle, queryFn: chargerFavoris, enabled: estMembre })
  const biens = data?.contenu ?? []
  const estFavori = (bienId) => biens.some((b) => b.id === Number(bienId))

  const bascule = useMutation({
    mutationFn: (bienId) => (estFavori(bienId) ? api.delete(`/biens/${bienId}/favori`) : api.put(`/biens/${bienId}/favori`)),
    onSettled: () => queryClient.invalidateQueries({ queryKey: cle }),
  })

  return { estMembre, biens, estFavori, basculer: bascule.mutate, enCours: bascule.isPending, isPending: estMembre && isPending, isError }
}
