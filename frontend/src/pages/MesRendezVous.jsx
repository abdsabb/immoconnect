import { useState } from 'react'
import { Link } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { changerStatut, chargerRendezVous, formatHeure, formatJour, formatMontant } from '../services/rendezVous'
import { erreursApi } from '../components/Formulaire'
import AgendaAgent from '../components/AgendaAgent'

// Badges alignés sur l'énumération StatutRendezVous (diagramme d'état-transition)
const BADGES = {
  demande: 'bg-ambre/30 text-nuit',
  confirme: 'bg-succes/10 text-succes',
  annule: 'bg-erreur/10 text-erreur',
  honore: 'bg-perle text-gray-700',
}

/**
 * Transitions proposées, par rôle — le serveur reste seul juge (RA1, RA2 et propriété du rendez-vous) :
 * l'interface ne fait que masquer les boutons qui seraient refusés.
 */
// RA8 et RA14 : un créneau payé est remboursé, sauf au membre qui annule moins de 24 h avant la visite
function messageAnnulation(rdv, role) {
  if (rdv.paiement?.statut !== 'reussi') return 'rdv.confirmerAnnulation'
  if (role !== 'agent' && rdv.remboursableJusquA && new Date(rdv.remboursableJusquA) < new Date()) return 'rdv.confirmerAnnulationTardive'
  return 'rdv.confirmerAnnulationPayee'
}

function actionsPossibles(rdv, role) {
  const futur = new Date(rdv.dateHeure) > new Date()
  const actif = rdv.statut === 'demande' || rdv.statut === 'confirme'
  const actions = []
  if (role === 'agent' && rdv.statut === 'demande' && futur) actions.push('confirmer')
  if (role === 'agent' && rdv.statut === 'confirme' && !futur) actions.push('honorer')
  if (actif && futur) actions.push('annuler')
  return actions
}

// Cas M4 (suivi de ses visites par le membre) et AG5 (gestion des demandes par l'agent)
export default function MesRendezVous() {
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  const { utilisateur } = useAuth()
  const role = utilisateur?.role
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState(null)
  const [vue, setVue] = useState('liste')
  // Un clic sur l'agenda mène au rendez-vous dans la liste, où se prennent les décisions
  const choisir = (id) => {
    setVue('liste')
    requestAnimationFrame(() => document.getElementById(`rdv-${id}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' }))
  }

  // La clé porte l'identifiant du compte : une liste n'est jamais servie à un autre utilisateur
  const { data, isPending, isError } = useQuery({
    queryKey: ['rendez-vous', utilisateur?.id],
    queryFn: chargerRendezVous,
    enabled: role !== 'admin',
  })
  const transition = useMutation({
    mutationFn: ({ id, action }) => changerStatut(id, action),
    onMutate: () => setErreur(null),
    onError: (e) => setErreur(erreursApi(e, t).message),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['rendez-vous'] })
      queryClient.invalidateQueries({ queryKey: ['creneaux'] })
    },
  })

  const executer = (rdv, action) => {
    if (action === 'annuler' && !window.confirm(t(messageAnnulation(rdv, role)))) return
    transition.mutate({ id: rdv.id, action })
  }

  const aVenir = (data ?? []).filter((r) => new Date(r.dateHeure) > new Date()).reverse()
  const passes = (data ?? []).filter((r) => new Date(r.dateHeure) <= new Date())

  return (
    <section className="mx-auto max-w-4xl px-4 py-10">
      <h1 className="text-3xl font-bold text-nuit">{t(role === 'agent' ? 'rdv.agenda' : 'rdv.mesVisites')}</h1>

      {erreur && <p role="alert" className="mt-6 rounded-lg bg-erreur/10 text-erreur px-4 py-3">{erreur}</p>}
      {role === 'admin' && <p className="mt-6 text-gray-600">{t('rdv.reserveAuxMembres')}</p>}
      {isPending && role !== 'admin' && <p className="mt-6 text-gray-500">{t('commun.chargement')}</p>}
      {isError && <p role="alert" className="mt-6 text-erreur">{t('commun.erreurReseau')}</p>}

      {data && role === 'agent' && (
        <div role="group" aria-label={t('rdv.affichage')} className="mt-6 flex gap-2">
          {['liste', 'agenda'].map((v) => (
            <button key={v} type="button" onClick={() => setVue(v)} aria-pressed={vue === v}
              className={`rounded-lg px-3 py-2 text-sm font-titre font-semibold ${vue === v ? 'bg-nuit text-white' : 'bg-perle text-nuit hover:bg-gray-200'}`}>
              {t(`rdv.vue.${v}`)}
            </button>
          ))}
        </div>
      )}
      {data && vue === 'agenda' && <AgendaAgent rendezVous={data} langue={langue} onChoix={choisir} />}

      {data && vue === 'liste' && (
        <>
          <Liste titre={t('rdv.aVenir')} vide={t('rdv.aucunAVenir')} rendezVous={aVenir} role={role} langue={langue}
            executer={executer} enCours={transition.isPending} />
          <Liste titre={t('rdv.passes')} vide={t('rdv.aucunPasse')} rendezVous={passes} role={role} langue={langue}
            executer={executer} enCours={transition.isPending} />
          {role === 'membre' && (
            <Link to="/biens" className="mt-8 inline-block text-turquoise font-semibold underline">{t('rdv.trouverUnBien')}</Link>
          )}
        </>
      )}
    </section>
  )
}

function Liste({ titre, vide, rendezVous, role, langue, executer, enCours }) {
  const { t } = useTranslation()
  return (
    <>
      <h2 className="mt-8 text-xl font-bold text-nuit">{titre} ({rendezVous.length})</h2>
      {rendezVous.length === 0 && <p className="mt-2 text-gray-600">{vide}</p>}
      <ul className="mt-3 space-y-3">
        {rendezVous.map((rdv) => (
          <li key={rdv.id} id={`rdv-${rdv.id}`} className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm scroll-mt-24">
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div>
                <p className="font-titre font-bold text-nuit first-letter:uppercase">
                  {formatJour(rdv.dateHeure, langue)} · {formatHeure(rdv.dateHeure, langue)}
                </p>
                <Link to={`/biens/${rdv.bienId}`} className="text-turquoise hover:underline">{rdv.bienTitre}</Link>
              </div>
              <div className="flex gap-2">
                {rdv.type === 'premium' && <span className="text-xs font-semibold px-2 py-1 rounded-full bg-ambre text-nuit">{t('rdv.premium')}</span>}
                <span className={`text-xs font-semibold px-2 py-1 rounded-full ${BADGES[rdv.statut]}`}>{t(`rdv.statut.${rdv.statut}`)}</span>
              </div>
            </div>

            <dl className="mt-3 grid sm:grid-cols-2 gap-x-6 gap-y-1 text-sm">
              <Detail titre={role === 'agent' ? t('rdv.membre') : t('bien.agent')} valeur={role === 'agent' ? rdv.membre : rdv.agent} />
              <Detail titre={t('rdv.adresse')} valeur={rdv.adresse ?? t('rdv.adresseApresConfirmation')} />
              {rdv.motif && <Detail titre={t('rdv.motifCourt')} valeur={rdv.motif} />}
              {rdv.paiement && (
                <Detail titre={t('rdv.paiement')}
                  valeur={`${formatMontant(rdv.paiement.montant, langue)} · ${t(`rdv.paiementStatut.${rdv.paiement.statut}`)}`} />
              )}
              {role !== 'agent' && rdv.paiement?.statut === 'reussi' && rdv.remboursableJusquA && (rdv.statut === 'demande' || rdv.statut === 'confirme') && (
                <Detail titre={t('rdv.annulationGratuite')}
                  valeur={new Date(rdv.remboursableJusquA) < new Date() ? t('rdv.annulationSansRemboursement') : t('rdv.remboursableJusquA', { date: `${formatJour(rdv.remboursableJusquA, langue)} ${formatHeure(rdv.remboursableJusquA, langue)}` })} />
              )}
            </dl>

            {actionsPossibles(rdv, role).length > 0 && (
              <div className="mt-4 flex flex-wrap gap-2">
                {actionsPossibles(rdv, role).map((action) => (
                  <button key={action} type="button" disabled={enCours} onClick={() => executer(rdv, action)}
                    className={`rounded-lg px-4 py-2 font-titre font-semibold disabled:opacity-60 ${
                      action === 'annuler' ? 'border-2 border-erreur text-erreur hover:bg-erreur/10' : 'bg-turquoise text-white hover:bg-turquoise/90'
                    }`}>
                    {t(`rdv.action.${action}`)}
                  </button>
                ))}
              </div>
            )}
          </li>
        ))}
      </ul>
    </>
  )
}

function Detail({ titre, valeur }) {
  return (
    <div className="flex gap-2">
      <dt className="text-gray-500">{titre} :</dt>
      <dd className="text-nuit">{valeur}</dd>
    </div>
  )
}
