import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { chargerConversation, envoyerMessage, marquerConversationLue, useConversations } from '../services/messages'
import { formatHeure, formatJour } from '../services/rendezVous'
import { erreursApi } from '../components/Formulaire'

const LONGUEUR_MAX = 5000

// Cas M3 (le membre écrit à un agent) et AG4 (l'agent répond) : liste des conversations et fil de discussion.
// Depuis la fiche d'un bien, le lien « Envoyer un message » ouvre la conversation avec son agent.
export default function Messagerie() {
  const { interlocuteurId } = useParams()
  const location = useLocation()
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  const { utilisateur } = useAuth()
  const { actif, conversations, isPending } = useConversations()
  const connue = conversations.find((c) => String(c.interlocuteurId) === interlocuteurId)
  // Première prise de contact : le nom de l'agent vient de la fiche du bien qui a conduit ici
  const interlocuteur = connue?.interlocuteur ?? location.state?.interlocuteur

  if (!actif) {
    return <p className="mx-auto max-w-5xl px-4 py-10 text-gray-600">{t('message.reserve')}</p>
  }
  return (
    <section className="mx-auto max-w-5xl px-4 py-10">
      <h1 className="text-3xl font-bold text-nuit">{t('message.titre')}</h1>
      <div className="mt-6 grid grid-cols-1 md:grid-cols-3 gap-6">
        <nav aria-label={t('message.conversations')} className={`md:col-span-1 ${interlocuteurId ? 'hidden md:block' : ''}`}>
          {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
          {!isPending && conversations.length === 0 && (
            <p className="text-gray-600">{t(utilisateur.role === 'agent' ? 'message.aucuneAgent' : 'message.aucune')}</p>
          )}
          <ul className="space-y-2">
            {conversations.map((c) => (
              <li key={c.interlocuteurId}>
                <Link to={`/messages/${c.interlocuteurId}`} aria-current={String(c.interlocuteurId) === interlocuteurId ? 'page' : undefined}
                  className={`block rounded-xl border p-3 ${String(c.interlocuteurId) === interlocuteurId ? 'border-turquoise bg-perle' : 'border-gray-200 hover:bg-perle'}`}>
                  <span className="flex items-center justify-between gap-2">
                    <span className={`font-titre text-nuit ${c.nonLus ? 'font-extrabold' : 'font-semibold'}`}>{c.interlocuteur}</span>
                    {c.nonLus > 0 && (
                      <span className="rounded-full bg-corail text-white text-xs font-bold px-2 py-0.5" aria-label={t('message.nonLus', { count: c.nonLus })}>
                        {c.nonLus}
                      </span>
                    )}
                  </span>
                  <span className="block text-sm text-gray-600 truncate">
                    {c.dernierDeMoi && `${t('message.vous')} : `}{c.dernierMessage}
                  </span>
                  <span className="block text-xs text-gray-400">{formatJour(c.dernierEnvoi, langue)}</span>
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <div className="md:col-span-2">
          {interlocuteurId
            ? <Fil key={interlocuteurId} interlocuteurId={interlocuteurId} interlocuteur={interlocuteur} brouillon={location.state?.brouillon} />
            : <p className="hidden md:block text-gray-500">{t('message.choisir')}</p>}
        </div>
      </div>
    </section>
  )
}

function Fil({ interlocuteurId, interlocuteur, brouillon }) {
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  const { utilisateur } = useAuth()
  const queryClient = useQueryClient()
  const [texte, setTexte] = useState(brouillon ?? '')
  const [erreur, setErreur] = useState(null)
  const fin = useRef(null)

  const { data: messages, isPending } = useQuery({
    queryKey: ['conversation', utilisateur.id, interlocuteurId],
    queryFn: () => chargerConversation(interlocuteurId),
    refetchInterval: 30_000,
  })

  // Ouvrir le fil vaut lecture : les messages reçus sont marqués lus, le compteur de la liste retombe
  const aLire = (messages ?? []).filter((m) => !m.deMoi && !m.lu).length
  useEffect(() => {
    if (aLire === 0) return
    marquerConversationLue(interlocuteurId).then(() => queryClient.invalidateQueries({ queryKey: ['conversations'] }))
  }, [aLire, interlocuteurId, queryClient])

  useEffect(() => {
    fin.current?.scrollIntoView({ block: 'nearest' })
  }, [messages?.length])

  const envoi = useMutation({
    mutationFn: () => envoyerMessage(interlocuteurId, texte.trim()),
    onMutate: () => setErreur(null),
    onSuccess: () => {
      setTexte('')
      queryClient.invalidateQueries({ queryKey: ['conversation', utilisateur.id, interlocuteurId] })
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
    },
    onError: (e) => {
      const { message, champs } = erreursApi(e, t)
      setErreur(Object.values(champs)[0] ?? message)
    },
  })

  const soumettre = (e) => {
    e.preventDefault()
    if (texte.trim()) envoi.mutate()
  }

  return (
    <div className="border border-gray-200 rounded-xl flex flex-col h-[32rem]">
      <header className="flex items-center gap-3 border-b border-gray-200 px-4 py-3">
        <Link to="/messages" className="md:hidden text-turquoise" aria-label={t('message.retour')}>‹</Link>
        <h2 className="font-titre font-bold text-nuit">{interlocuteur ?? t('message.conversation')}</h2>
      </header>

      <ol className="flex-1 overflow-y-auto px-4 py-3 space-y-3" aria-live="polite">
        {isPending && <li className="text-gray-500">{t('commun.chargement')}</li>}
        {messages?.length === 0 && <li className="text-gray-500">{t('message.premier')}</li>}
        {messages?.map((m) => (
          <li key={m.id} className={`flex ${m.deMoi ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[80%] rounded-2xl px-4 py-2 ${m.deMoi ? 'bg-turquoise text-white' : 'bg-perle text-nuit'}`}>
              <p className="whitespace-pre-line break-words">{m.contenu}</p>
              <p className={`mt-1 text-xs ${m.deMoi ? 'text-white/80' : 'text-gray-500'}`}>
                {formatJour(m.envoyeLe, langue)} · {formatHeure(m.envoyeLe, langue)}
                {m.deMoi && m.lu && ` · ${t('message.lu')}`}
              </p>
            </div>
          </li>
        ))}
        <li ref={fin} aria-hidden="true" />
      </ol>

      <form onSubmit={soumettre} className="border-t border-gray-200 p-3 space-y-2">
        {erreur && <p role="alert" className="text-erreur text-sm">{erreur}</p>}
        <div className="flex gap-2">
          <label className="flex-1">
            <span className="sr-only">{t('message.votreMessage')}</span>
            <textarea rows="2" maxLength={LONGUEUR_MAX} value={texte} onChange={(e) => setTexte(e.target.value)}
              placeholder={t('message.votreMessage')} className="w-full rounded-lg border border-gray-300 px-3 py-2" />
          </label>
          <button type="submit" disabled={envoi.isPending || !texte.trim()}
            className="self-end bg-corail hover:bg-corail/90 disabled:opacity-60 text-white font-titre font-bold rounded-lg px-4 py-3">
            {t('message.envoyer')}
          </button>
        </div>
      </form>
    </div>
  )
}
