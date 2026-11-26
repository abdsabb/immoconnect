import { useMemo, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Champ, classeInput, erreursApi } from './Formulaire'
import { formatHeure, formatJour, jourDe } from '../services/rendezVous'
import {
  creneauxDeLaPlage, demiHeures, FERMETURE, OUVERTURE, ouvrirDisponibilite, retirerDisponibilite, retirerPeriode,
} from '../services/disponibilites'

const HORIZON_JOURS = 60
// La liste montre d'abord les deux prochaines semaines : un agenda ouvert sur deux mois compte des dizaines de plages
const JOURS_AFFICHES = 14
const jourIso = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const dansJours = (jours) => { const d = new Date(); d.setDate(d.getDate() + jours); return jourIso(d) }

/**
 * Cas AG7 « Gérer ses disponibilités » : l'agent ouvre les plages pendant lesquelles il reçoit en visite, et les
 * retire s'il n'est plus disponible. Les membres ne voient que les créneaux de ces plages.
 * Retirer une plage où des visites sont prévues demande une confirmation : le serveur répond d'abord 409 avec
 * leur nombre, et ne les annule que si l'agent confirme.
 */
export default function Disponibilites({ disponibilites }) {
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  const queryClient = useQueryClient()
  const [plage, setPlage] = useState({ jour: dansJours(1), de: '09:00', a: '12:00', semaines: 1 })
  const [absence, setAbsence] = useState({ du: dansJours(0), au: dansJours(0) })
  const [erreur, setErreur] = useState(null)
  const [message, setMessage] = useState(null)
  const [enCours, setEnCours] = useState(false)
  const [tout, setTout] = useState(false)

  const aVenir = useMemo(() => disponibilites.filter((d) => new Date(d.fin) > new Date()), [disponibilites])
  const proches = useMemo(() => aVenir.filter((d) => jourDe(d.debut) <= dansJours(JOURS_AFFICHES)), [aVenir])
  const affichees = tout ? aVenir : proches
  const apercu = creneauxDeLaPlage(plage.de, plage.a)

  const rafraichir = () => {
    queryClient.invalidateQueries({ queryKey: ['disponibilites'] })
    queryClient.invalidateQueries({ queryKey: ['rendez-vous'] })
    queryClient.invalidateQueries({ queryKey: ['creneaux'] })
  }

  const executer = async (action) => {
    setErreur(null)
    setMessage(null)
    setEnCours(true)
    try {
      setMessage(await action())
      rafraichir()
    } catch (e) {
      const { message: texte, champs } = erreursApi(e, t)
      setErreur(Object.values(champs)[0] ?? texte)
    } finally {
      setEnCours(false)
    }
  }

  const ouvrir = (e) => {
    e.preventDefault()
    executer(async () => {
      const ouvertes = await ouvrirDisponibilite({
        debut: `${plage.jour}T${plage.de}:00`, fin: `${plage.jour}T${plage.a}:00`, semaines: Number(plage.semaines),
      })
      return t('dispo.ouverte', { count: ouvertes.length })
    })
  }

  // Le premier appel n'annule rien : si des visites sont prévues, l'agent confirme avant le second
  const retirer = (appel) => executer(async () => {
    let bilan
    try {
      bilan = await appel(false)
    } catch (e) {
      const visites = e.response?.data?.visites
      if (e.response?.status !== 409 || !visites) throw e
      if (!window.confirm(t('dispo.confirmerAnnulation', { count: visites }))) return null
      bilan = await appel(true)
    }
    if (bilan.plagesRetirees === 0) return t('dispo.rienARetirer')
    return bilan.visitesAnnulees > 0
      ? t('dispo.retireeAvecVisites', { count: bilan.visitesAnnulees })
      : t('dispo.retiree', { count: bilan.plagesRetirees })
  })

  return (
    <div className="mt-6 space-y-8">
      <p className="rounded-lg bg-perle px-4 py-3 text-sm text-gray-700">{t('dispo.explication')}</p>
      {erreur && <p role="alert" className="rounded-lg bg-erreur/10 px-4 py-3 text-erreur">{erreur}</p>}
      {message && <p role="status" className="rounded-lg bg-succes/10 px-4 py-3 font-semibold text-succes">{message}</p>}

      <form onSubmit={ouvrir} className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
        <h2 className="text-xl font-bold text-nuit">{t('dispo.ouvrir')}</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-4">
          <Champ label={t('dispo.jour')}>
            <input type="date" required value={plage.jour} min={dansJours(0)} max={dansJours(HORIZON_JOURS)}
              onChange={(e) => setPlage({ ...plage, jour: e.target.value })} className={classeInput()} />
          </Champ>
          <Champ label={t('dispo.de')}>
            <select value={plage.de} onChange={(e) => setPlage({ ...plage, de: e.target.value })} className={classeInput()}>
              {demiHeures(OUVERTURE, FERMETURE - 60).map((h) => <option key={h}>{h}</option>)}
            </select>
          </Champ>
          <Champ label={t('dispo.a')}>
            <select value={plage.a} onChange={(e) => setPlage({ ...plage, a: e.target.value })} className={classeInput()}>
              {demiHeures(OUVERTURE + 60, FERMETURE).map((h) => <option key={h}>{h}</option>)}
            </select>
          </Champ>
          <Champ label={t('dispo.repeter')}>
            <select value={plage.semaines} onChange={(e) => setPlage({ ...plage, semaines: e.target.value })} className={classeInput()}>
              {[1, 2, 3, 4, 5, 6, 7, 8].map((n) => <option key={n} value={n}>{t('dispo.semaines', { count: n })}</option>)}
            </select>
          </Champ>
        </div>
        <p className="mt-3 text-sm text-gray-600" data-testid="apercu">
          {apercu.length > 0 ? t('dispo.apercu', { count: apercu.length, heures: apercu.join(', ') }) : t('dispo.apercuVide')}
        </p>
        <button type="submit" disabled={enCours || apercu.length === 0}
          className="mt-4 rounded-lg bg-turquoise px-4 py-2 font-titre font-semibold text-white hover:bg-turquoise/90 disabled:opacity-60">
          {t('dispo.boutonOuvrir')}
        </button>
      </form>

      <section>
        <h2 className="text-xl font-bold text-nuit">{t('dispo.mesPlages')} ({aVenir.length})</h2>
        {aVenir.length === 0 && <p role="alert" className="mt-2 rounded-lg bg-ambre/30 px-4 py-3 text-nuit">{t('dispo.aucune')}</p>}
        <ul className="mt-3 divide-y divide-gray-200 rounded-xl border border-gray-200 bg-white shadow-sm">
          {affichees.map((d) => (
            <li key={d.id} className="flex flex-wrap items-center justify-between gap-2 px-4 py-2 text-sm">
              <span>
                <span className="font-titre font-bold text-nuit first-letter:uppercase">{formatJour(d.debut, langue)}</span>
                {' · '}{formatHeure(d.debut, langue)} – {formatHeure(d.fin, langue)}
                <span className="ml-2 text-gray-500">{t('dispo.creneaux', { count: d.creneaux })}</span>
              </span>
              <button type="button" disabled={enCours} onClick={() => retirer((annuler) => retirerDisponibilite(d.id, annuler))}
                aria-label={t('dispo.retirerLe', { jour: jourDe(d.debut) })}
                className="rounded-lg border-2 border-erreur px-3 py-1 font-titre font-semibold text-erreur hover:bg-erreur/10 disabled:opacity-60">
                {t('dispo.retirer')}
              </button>
            </li>
          ))}
        </ul>
        {aVenir.length > proches.length && (
          <button type="button" onClick={() => setTout(!tout)} className="mt-3 font-semibold text-turquoise underline">
            {tout ? t('dispo.voirMoins') : t('dispo.voirTout', { count: aVenir.length })}
          </button>
        )}
      </section>

      <form onSubmit={(e) => { e.preventDefault(); retirer((annuler) => retirerPeriode(absence.du, absence.au, annuler)) }}
        className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
        <h2 className="text-xl font-bold text-nuit">{t('dispo.indisponible')}</h2>
        <p className="mt-1 text-sm text-gray-600">{t('dispo.indisponibleExplication')}</p>
        <div className="mt-4 grid gap-4 sm:grid-cols-3">
          <Champ label={t('dispo.du')}>
            <input type="date" required value={absence.du} min={dansJours(0)}
              onChange={(e) => setAbsence({ du: e.target.value, au: e.target.value > absence.au ? e.target.value : absence.au })} className={classeInput()} />
          </Champ>
          <Champ label={t('dispo.au')}>
            <input type="date" required value={absence.au} min={absence.du}
              onChange={(e) => setAbsence({ ...absence, au: e.target.value })} className={classeInput()} />
          </Champ>
          <div className="flex items-end">
            <button type="submit" disabled={enCours}
              className="w-full rounded-lg border-2 border-erreur px-4 py-2 font-titre font-semibold text-erreur hover:bg-erreur/10 disabled:opacity-60">
              {t('dispo.boutonIndisponible')}
            </button>
          </div>
        </div>
      </form>
    </div>
  )
}
