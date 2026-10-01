import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerComptes, chargerConversationAgent, chargerConversationsAgent, formatDateHeure } from '../../services/admin'
import { erreursApi } from '../../components/Formulaire'
import { Avis } from './Administration'

// Supervision : la direction de l'agence lit les échanges de ses agents avec les clients, sans y écrire.
// Rien n'est marqué comme lu, et chaque consultation est inscrite au journal d'audit.
export default function MessagerieAgents() {
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  const [agentId, setAgentId] = useState('')
  const [membreId, setMembreId] = useState(null)
  const agents = useQuery({ queryKey: ['admin', 'agents'], queryFn: () => chargerComptes({ role: 'agent', taille: 100 }) })
  const conversations = useQuery({
    queryKey: ['admin', 'conversations', agentId],
    queryFn: () => chargerConversationsAgent(agentId),
    enabled: agentId !== '',
  })
  const messages = useQuery({
    queryKey: ['admin', 'conversation', agentId, membreId],
    queryFn: () => chargerConversationAgent(agentId, membreId),
    enabled: agentId !== '' && membreId !== null,
  })
  const erreur = conversations.error ?? messages.error

  return (
    <div className="space-y-4">
      <p className="text-sm text-gray-600">{t('admin.messagerie.explication')}</p>
      <label className="block max-w-sm text-sm font-semibold text-nuit">{t('admin.annonces.agent')}
        <select value={agentId} onChange={(e) => { setAgentId(e.target.value); setMembreId(null) }}
          className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 font-normal">
          <option value="">{t('admin.messagerie.choisirAgent')}</option>
          {agents.data?.contenu.map((a) => <option key={a.id} value={a.id}>{a.prenom} {a.nom}</option>)}
        </select>
      </label>
      {erreur && <Avis erreur={erreursApi(erreur, t).message} />}

      {agentId !== '' && (
        <div className="grid gap-4 md:grid-cols-3">
          <div className="md:col-span-1">
            <h2 className="font-titre font-bold text-nuit">{t('message.conversations')}</h2>
            {conversations.isPending && <p className="mt-2 text-gray-500">{t('commun.chargement')}</p>}
            {conversations.data?.length === 0 && <p className="mt-2 text-sm text-gray-600">{t('admin.messagerie.aucune')}</p>}
            <ul className="mt-2 space-y-2">
              {conversations.data?.map((c) => (
                <li key={c.interlocuteurId}>
                  <button type="button" onClick={() => setMembreId(c.interlocuteurId)} aria-pressed={membreId === c.interlocuteurId}
                    className={`w-full rounded-lg border px-3 py-2 text-left text-sm ${membreId === c.interlocuteurId ? 'border-turquoise bg-turquoise/10' : 'border-gray-200 hover:bg-perle'}`}>
                    <span className="flex items-center justify-between gap-2">
                      <strong className="text-nuit">{c.interlocuteur}</strong>
                      {c.nonLus > 0 && <span className="rounded-full bg-corail px-2 text-xs font-semibold text-white">{c.nonLus}</span>}
                    </span>
                    <span className="block truncate text-gray-600">{c.dernierMessage}</span>
                    <span className="block text-xs text-gray-500">{formatDateHeure(c.dernierEnvoi, langue)}</span>
                  </button>
                </li>
              ))}
            </ul>
          </div>

          <div className="md:col-span-2">
            {membreId === null && <p className="text-sm text-gray-600">{t('admin.messagerie.choisirConversation')}</p>}
            {membreId !== null && messages.isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
            <ol className="space-y-3">
              {messages.data?.map((m) => (
                // « deMoi » est vu du côté de l'agent : ses messages à droite, ceux du client à gauche
                <li key={m.id} className={`flex ${m.deMoi ? 'justify-end' : 'justify-start'}`}>
                  <div className={`max-w-[85%] rounded-2xl px-4 py-2 ${m.deMoi ? 'bg-turquoise text-white' : 'bg-perle text-nuit'}`}>
                    <p className={`text-xs font-semibold ${m.deMoi ? 'text-white/90' : 'text-gray-600'}`}>{m.expediteur}</p>
                    <p className="whitespace-pre-line break-words">{m.contenu}</p>
                    <p className={`mt-1 text-xs ${m.deMoi ? 'text-white/80' : 'text-gray-500'}`}>
                      {formatDateHeure(m.envoyeLe, langue)}{m.lu ? ` · ${t('message.lu')}` : ''}
                    </p>
                  </div>
                </li>
              ))}
            </ol>
          </div>
        </div>
      )}
    </div>
  )
}
