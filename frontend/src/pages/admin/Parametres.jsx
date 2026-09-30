import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerParametres, enregistrerParametres, formatDateHeure } from '../../services/admin'
import { erreursApi } from '../../components/Formulaire'
import { Avis, classeBouton } from './Administration'

const LANGUES = ['fr', 'nl', 'en']
const CHAMPS_TEXTE = ['agence.nom', 'agence.slogan', 'agence.adresse', 'agence.telephone', 'agence.email', 'agence.horaires']

// Cas A5 « Configurer les paramètres du site » : identité de l'agence et langues actives (A6).
// Les secrets (Stripe, courriel) ne passent jamais par ici : ils restent sur le serveur.
export default function Parametres() {
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  const queryClient = useQueryClient()
  const [brouillon, setBrouillon] = useState(null)
  const [avis, setAvis] = useState({})
  const [erreurs, setErreurs] = useState({})
  const [enCours, setEnCours] = useState(false)
  const { data, isPending, error } = useQuery({ queryKey: ['admin', 'parametres'], queryFn: chargerParametres })

  const valeurs = brouillon ?? Object.fromEntries((data ?? []).map((p) => [p.cle, p.valeur]))
  const languesActives = (valeurs['langues.actives'] ?? '').split(',').map((l) => l.trim()).filter(Boolean)
  const modifier = (cle, valeur) => setBrouillon({ ...valeurs, [cle]: valeur })
  const basculerLangue = (l) => {
    const actives = languesActives.includes(l) ? languesActives.filter((x) => x !== l) : [...languesActives, l]
    modifier('langues.actives', LANGUES.filter((x) => actives.includes(x)).join(','))
  }

  const enregistrer = async (e) => {
    e.preventDefault()
    setAvis({})
    setErreurs({})
    setEnCours(true)
    try {
      await enregistrerParametres(valeurs)
      setBrouillon(null)
      setAvis({ message: t('admin.parametre.enregistre') })
      queryClient.invalidateQueries({ queryKey: ['admin', 'parametres'] })
      queryClient.invalidateQueries({ queryKey: ['configuration'] })
    } catch (err) {
      const { message, champs } = erreursApi(err, t)
      setErreurs(champs)
      setAvis({ erreur: Object.keys(champs).length ? t('annonce.corriger') : message })
    } finally {
      setEnCours(false)
    }
  }

  const derniereModification = (data ?? []).reduce((max, p) => (!max || p.modifieLe > max.modifieLe ? p : max), null)

  return (
    <form onSubmit={enregistrer} className="space-y-4">
      <p className="text-sm text-gray-600">{t('admin.parametre.explication')}</p>
      <Avis {...avis} />
      {error && <Avis erreur={erreursApi(error, t).message} />}
      {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
      {data && (
        <>
          <div className="grid gap-4 md:grid-cols-2">
            {CHAMPS_TEXTE.map((cle) => (
              <label key={cle} className={`text-sm font-semibold text-nuit ${cle === 'agence.horaires' || cle === 'agence.adresse' ? 'md:col-span-2' : ''}`}>
                {t(`admin.parametre.cles.${cle}`)}
                <input value={valeurs[cle] ?? ''} onChange={(e) => modifier(cle, e.target.value)} maxLength="1000"
                  type={cle === 'agence.email' ? 'email' : 'text'}
                  className={`mt-1 block w-full rounded-lg border px-3 py-2 font-normal ${erreurs[cle] ? 'border-erreur' : 'border-gray-300'}`} />
                {erreurs[cle] && <span role="alert" className="text-xs font-normal text-erreur">{erreurs[cle]}</span>}
              </label>
            ))}
          </div>

          <fieldset className="rounded-xl bg-perle p-4">
            <legend className="px-2 text-sm font-semibold text-nuit">{t('admin.parametre.cles.langues.actives')}</legend>
            <p className="text-xs text-gray-600">{t('admin.parametre.languesExplication')}</p>
            <div className="mt-2 flex flex-wrap gap-4">
              {LANGUES.map((l) => (
                <label key={l} className="flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={languesActives.includes(l)} disabled={l === 'fr'} onChange={() => basculerLangue(l)} />
                  {t(`langue.${l}`, { defaultValue: l.toUpperCase() })}
                </label>
              ))}
            </div>
            {erreurs['langues.actives'] && <p role="alert" className="mt-1 text-xs text-erreur">{erreurs['langues.actives']}</p>}
          </fieldset>

          <div className="flex flex-wrap items-center gap-4">
            <button type="submit" disabled={enCours || !brouillon} className={`${classeBouton} bg-corail text-white`}>
              {t('admin.parametre.enregistrer')}
            </button>
            {brouillon && (
              <button type="button" onClick={() => { setBrouillon(null); setErreurs({}) }} className={`${classeBouton} text-gray-600 hover:bg-perle`}>
                {t('commun.annuler')}
              </button>
            )}
            {derniereModification?.modifiePar && (
              <span className="text-xs text-gray-500">
                {t('admin.parametre.derniereModification', { qui: derniereModification.modifiePar, quand: formatDateHeure(derniereModification.modifieLe, langue) })}
              </span>
            )}
          </div>
        </>
      )}
    </form>
  )
}
