import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { chargerBien } from '../services/biens'
import {
  chargerConfigPaiement, chargerCreneaux, formatHeure, formatJour, formatMontant, jourDe, preparerPaiement, reserver,
} from '../services/rendezVous'
import { BoutonPrincipal, Champ, classeInput, erreursApi } from '../components/Formulaire'
import PaiementStripe from '../components/PaiementStripe'
import PaiementSimule from '../components/PaiementSimule'

// Cas M4 « Prendre rendez-vous pour visiter un bien » — le scénario validé dans l'analyse (livrable 07, V3) :
// choix du créneau → récapitulatif → paiement (créneau premium seulement) → confirmation.
const ETAPES = ['creneau', 'recapitulatif', 'paiement', 'confirmation']

export default function PriseRendezVous() {
  const { id } = useParams()
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  const { utilisateur } = useAuth()
  const queryClient = useQueryClient()
  const estMembre = utilisateur?.role === 'membre'

  const [etape, setEtape] = useState('creneau')
  const [creneau, setCreneau] = useState(null)
  const [motif, setMotif] = useState('')
  const [intention, setIntention] = useState(null)
  const [rendezVous, setRendezVous] = useState(null)
  const [erreur, setErreur] = useState(null)
  const [bienRetire, setBienRetire] = useState(false)
  const [enCours, setEnCours] = useState(false)

  const bien = useQuery({ queryKey: ['bien', id], queryFn: () => chargerBien(id) })
  const creneaux = useQuery({ queryKey: ['creneaux', id], queryFn: () => chargerCreneaux(id), enabled: estMembre })
  const paiement = useQuery({ queryKey: ['paiement-config'], queryFn: chargerConfigPaiement, staleTime: Infinity, enabled: estMembre })

  const jours = useMemo(() => {
    const parJour = new Map()
    for (const c of creneaux.data ?? []) {
      if (!parJour.has(jourDe(c.dateHeure))) parJour.set(jourDe(c.dateHeure), [])
      parJour.get(jourDe(c.dateHeure)).push(c)
    }
    return [...parJour.values()]
  }, [creneaux.data])

  // A2 (créneau pris entre-temps) ramène au choix du créneau ; E3 (bien retiré) renvoie vers la recherche
  const traiterEchec = (e, dejaPaye = false) => {
    const { message, champs, type } = erreursApi(e, t)
    // Le membre avait déjà payé : l'API l'a remboursé avant de répondre, il faut le lui dire
    const texte = Object.values(champs)[0] ?? message
    setErreur(dejaPaye && type !== 'validation' ? `${texte} ${t('rdv.rembourse')}` : texte)
    if (type === 'bien-indisponible' || e.response?.status === 404) {
      setBienRetire(true)
      return
    }
    setIntention(null)
    setCreneau(null)
    setEtape('creneau')
    queryClient.invalidateQueries({ queryKey: ['creneaux', id] })
  }

  const demande = () => ({ bienId: Number(id), dateHeure: creneau.dateHeure, motif: motif.trim() || null })

  const confirmer = async () => {
    setErreur(null)
    setEnCours(true)
    try {
      if (creneau.type === 'premium') {
        setIntention(await preparerPaiement(demande()))
        setEtape('paiement')
      } else {
        terminer(await reserver(demande()))
      }
    } catch (e) {
      traiterEchec(e)
    } finally {
      setEnCours(false)
    }
  }

  const apresPaiement = async () => {
    try {
      terminer(await reserver({ ...demande(), paymentIntentId: intention.paymentIntentId }))
    } catch (e) {
      traiterEchec(e, true)
    }
  }

  const terminer = (rdv) => {
    setRendezVous(rdv)
    setEtape('confirmation')
    queryClient.invalidateQueries({ queryKey: ['creneaux', id] })
    queryClient.invalidateQueries({ queryKey: ['rendez-vous'] })
  }

  if (bien.isPending) return <p className="mx-auto max-w-3xl px-4 py-10 text-gray-500">{t('commun.chargement')}</p>
  if (bien.error || bienRetire) {
    return (
      <section className="mx-auto max-w-3xl px-4 py-10">
        <p role="alert" className="text-erreur">{erreur ?? t('bien.introuvable')}</p>
        <Link to="/biens" className="mt-4 inline-block text-turquoise underline">{t('bien.retour')}</Link>
      </section>
    )
  }

  return (
    <section className="mx-auto max-w-3xl px-4 py-8">
      <nav aria-label="Fil d'Ariane" className="text-sm text-gray-500">
        <Link to="/biens" className="hover:text-turquoise">{t('nav.biens')}</Link> › <Link to={`/biens/${id}`} className="hover:text-turquoise">{bien.data.titre}</Link> › <span className="text-nuit">{t('bien.prendreRdv')}</span>
      </nav>
      <h1 className="mt-3 text-3xl font-bold text-nuit">{t('rdv.titre')}</h1>
      <p className="text-gray-600">{bien.data.titre} · {t('rdv.avec', { agent: bien.data.agent.nomComplet })}</p>

      <ol className="mt-6 flex gap-2 text-sm" aria-label={t('rdv.etapes')}>
        {ETAPES.filter((e) => e !== 'paiement' || creneau?.type === 'premium').map((e, i) => (
          <li key={e} aria-current={e === etape ? 'step' : undefined}
            className={`flex-1 rounded-full px-3 py-1 text-center font-semibold ${e === etape ? 'bg-turquoise text-white' : 'bg-perle text-gray-500'}`}>
            {i + 1}. {t(`rdv.etape.${e}`)}
          </li>
        ))}
      </ol>

      {erreur && <p role="alert" className="mt-6 rounded-lg bg-erreur/10 text-erreur px-4 py-3">{erreur}</p>}

      {!estMembre && <p className="mt-6 rounded-lg bg-perle px-4 py-3 text-gray-700">{t('rdv.reserveAuxMembres')}</p>}

      {estMembre && etape === 'creneau' && (
        <div className="mt-6 space-y-6">
          <p className="text-sm text-gray-600">{t('rdv.legende', { prix: formatMontant(paiement.data?.prixCreneauPremium ?? 15, langue) })}</p>
          {creneaux.isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
          {creneaux.isError && <p role="alert" className="text-erreur">{erreursApi(creneaux.error, t).message}</p>}
          {creneaux.data?.length === 0 && <p className="text-gray-600">{t('rdv.aucunCreneau')}</p>}
          {jours.map((creneauxDuJour) => (
            <fieldset key={jourDe(creneauxDuJour[0].dateHeure)}>
              <legend className="font-titre font-bold text-nuit first-letter:uppercase">{formatJour(creneauxDuJour[0].dateHeure, langue)}</legend>
              <div className="mt-2 flex flex-wrap gap-2">
                {creneauxDuJour.map((c) => (
                  <button key={c.dateHeure} type="button" onClick={() => setCreneau(c)} aria-pressed={creneau?.dateHeure === c.dateHeure}
                    className={`rounded-lg border-2 px-3 py-2 text-sm font-semibold ${
                      creneau?.dateHeure === c.dateHeure ? 'border-corail bg-corail text-white'
                        : c.type === 'premium' ? 'border-ambre text-nuit hover:bg-ambre/20' : 'border-gray-300 text-nuit hover:bg-perle'
                    }`}>
                    {formatHeure(c.dateHeure, langue)}
                    {c.type === 'premium' && <span className="ml-2 text-xs font-normal">{t('rdv.premium')} · {formatMontant(c.prix, langue)}</span>}
                  </button>
                ))}
              </div>
            </fieldset>
          ))}
          {creneau && (
            <form onSubmit={(e) => { e.preventDefault(); setErreur(null); setEtape('recapitulatif') }} className="space-y-4 border-t border-gray-200 pt-6">
              <Champ label={t('rdv.motif')}>
                <textarea rows="3" maxLength="255" value={motif} onChange={(e) => setMotif(e.target.value)}
                  placeholder={t('rdv.motifExemple')} className={classeInput()} />
              </Champ>
              <BoutonPrincipal>{t('rdv.continuer')}</BoutonPrincipal>
            </form>
          )}
        </div>
      )}

      {estMembre && etape === 'recapitulatif' && (
        <div className="mt-6 space-y-4">
          <Recapitulatif bien={bien.data} creneau={creneau} motif={motif} />
          <p className="text-sm text-gray-600">{t(creneau.type === 'premium' ? 'rdv.explicationPremium' : 'rdv.explicationStandard')}</p>
          <BoutonPrincipal type="button" onClick={confirmer} chargement={enCours}>
            {creneau.type === 'premium' ? t('rdv.passerAuPaiement') : t('rdv.envoyerDemande')}
          </BoutonPrincipal>
          <button type="button" onClick={() => setEtape('creneau')} className="w-full text-center text-turquoise underline">
            {t('rdv.changerCreneau')}
          </button>
        </div>
      )}

      {estMembre && etape === 'paiement' && intention && (
        <div className="mt-6 space-y-6">
          <Recapitulatif bien={bien.data} creneau={creneau} motif={motif} />
          {paiement.data?.mode === 'stripe'
            ? <PaiementStripe clePublique={paiement.data.clePublique} intention={intention} onPaye={apresPaiement} />
            : <PaiementSimule intention={intention} onPaye={apresPaiement} />}
          <button type="button" onClick={() => { setIntention(null); setEtape('creneau') }} className="w-full text-center text-turquoise underline">
            {t('rdv.abandonner')}
          </button>
        </div>
      )}

      {etape === 'confirmation' && rendezVous && (
        <div className="mt-6 space-y-4">
          <p role="status" className="rounded-lg bg-succes/10 text-succes px-4 py-3 font-semibold">
            {t(rendezVous.statut === 'confirme' ? 'rdv.confirme' : 'rdv.demandeEnvoyee', { agent: rendezVous.agent })}
          </p>
          <dl className="bg-perle rounded-xl p-4 grid sm:grid-cols-2 gap-4">
            <Ligne titre={t('rdv.date')} valeur={`${formatJour(rendezVous.dateHeure, langue)} · ${formatHeure(rendezVous.dateHeure, langue)}`} />
            <Ligne titre={t('rdv.statutLabel')} valeur={t(`rdv.statut.${rendezVous.statut}`)} />
            {rendezVous.adresse && <Ligne titre={t('rdv.adresse')} valeur={rendezVous.adresse} />}
            {rendezVous.paiement && <Ligne titre={t('rdv.paiement')} valeur={formatMontant(rendezVous.paiement.montant, langue)} />}
          </dl>
          <Link to="/rendez-vous" className="block text-center bg-corail hover:bg-corail/90 text-white font-titre font-bold rounded-lg px-4 py-3">
            {t('rdv.voirMesVisites')}
          </Link>
        </div>
      )}
    </section>
  )
}

function Recapitulatif({ bien, creneau, motif }) {
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  return (
    <dl className="bg-perle rounded-xl p-4 grid sm:grid-cols-2 gap-4">
      <Ligne titre={t('rdv.bien')} valeur={bien.titre} />
      <Ligne titre={t('bien.agent')} valeur={bien.agent.nomComplet} />
      <Ligne titre={t('rdv.date')} valeur={`${formatJour(creneau.dateHeure, langue)} · ${formatHeure(creneau.dateHeure, langue)}`} />
      <Ligne titre={t('rdv.prix')} valeur={creneau.type === 'premium' ? formatMontant(creneau.prix, langue) : t('rdv.gratuit')} />
      {motif.trim() && <Ligne titre={t('rdv.motifCourt')} valeur={motif.trim()} />}
    </dl>
  )
}

function Ligne({ titre, valeur }) {
  return (
    <div>
      <dt className="text-xs uppercase text-gray-500">{titre}</dt>
      <dd className="font-semibold text-nuit">{valeur}</dd>
    </div>
  )
}
