import { useQuery } from '@tanstack/react-query'
import { api } from './api'
import { useAuth } from '../auth/AuthContext'

// Cas M3 / AG4 : messagerie entre membres et agents (livrable 15, §6.4)

export async function chargerConversations() {
  const { data } = await api.get('/messages')
  return data
}

export async function chargerConversation(interlocuteurId) {
  const { data } = await api.get(`/messages/conversations/${interlocuteurId}`)
  return data
}

export async function envoyerMessage(destinataireId, contenu) {
  const { data } = await api.post('/messages', { destinataireId: Number(destinataireId), contenu })
  return data
}

export async function marquerConversationLue(interlocuteurId) {
  await api.patch(`/messages/conversations/${interlocuteurId}/lu`)
}

/** Conversations du compte connecté, relues chaque minute pour signaler les nouveaux messages. */
export function useConversations() {
  const { utilisateur } = useAuth()
  const actif = utilisateur?.role === 'membre' || utilisateur?.role === 'agent'
  const requete = useQuery({
    queryKey: ['conversations', utilisateur?.id],
    queryFn: chargerConversations,
    enabled: actif,
    refetchInterval: 60_000,
  })
  const nonLus = (requete.data ?? []).reduce((total, c) => total + c.nonLus, 0)
  return { ...requete, actif, conversations: requete.data ?? [], nonLus }
}
