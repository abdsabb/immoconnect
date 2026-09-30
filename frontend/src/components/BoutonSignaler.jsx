import { useState } from 'react'
import { Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { signaler } from '../services/configuration'
import { erreursApi } from './Formulaire'

const MOTIFS = ['illicite', 'arnaque', 'indesirable', 'autre']

// Signalement d'un contenu (message reçu, annonce, article) : le règlement européen sur les services
// numériques demande un moyen simple de signaler, et une décision motivée de l'exploitant (chapitre 11).
export default function BoutonSignaler({ typeContenu, contenuId, discret = false, className = '' }) {
  const { t } = useTranslation()
  const { estConnecte } = useAuth()
  const [ouvert, setOuvert] = useState(false)
  const [motif, setMotif] = useState('illicite')
  const [description, setDescription] = useState('')
  const [etat, setEtat] = useState({})

  const envoyer = async (e) => {
    e.preventDefault()
    setEtat({ enCours: true })
    try {
      await signaler({ typeContenu, contenuId, motif, description: description.trim() })
      setEtat({ fait: true })
      setOuvert(false)
    } catch (err) {
      const { message, champs } = erreursApi(err, t)
      setEtat({ erreur: Object.values(champs)[0] ?? message })
    }
  }

  const bouton = discret
    ? 'text-xs text-gray-500 underline hover:text-erreur'
    : 'text-sm font-semibold text-gray-600 underline hover:text-erreur'

  if (etat.fait) return <p role="status" className={`text-xs text-succes ${className}`}>{t('signalement.merci')}</p>
  if (!estConnecte) {
    return <Link to="/connexion" className={`${bouton} ${className}`}>{t('signalement.bouton')}</Link>
  }
  return (
    <div className={className}>
      <button type="button" onClick={() => setOuvert(!ouvert)} aria-expanded={ouvert} className={bouton}>
        {t('signalement.bouton')}
      </button>
      {ouvert && (
        <form onSubmit={envoyer} className="mt-2 space-y-2 rounded-lg border border-gray-200 bg-white p-3 text-sm shadow-sm">
          <p className="font-semibold text-nuit">{t('signalement.titre')}</p>
          <label className="block">
            <span className="text-xs font-semibold text-gray-600">{t('signalement.motif')}</span>
            <select value={motif} onChange={(e) => setMotif(e.target.value)} className="mt-1 block w-full rounded-lg border border-gray-300 px-2 py-1">
              {MOTIFS.map((m) => <option key={m} value={m}>{t(`signalement.motifs.${m}`)}</option>)}
            </select>
          </label>
          <label className="block">
            <span className="text-xs font-semibold text-gray-600">{t('signalement.description')}</span>
            <textarea required maxLength="1000" rows="3" value={description} onChange={(e) => setDescription(e.target.value)}
              className="mt-1 block w-full rounded-lg border border-gray-300 px-2 py-1" />
          </label>
          {etat.erreur && <p role="alert" className="text-erreur">{etat.erreur}</p>}
          <div className="flex gap-2">
            <button type="submit" disabled={etat.enCours} className="rounded-lg bg-nuit px-3 py-1 font-semibold text-white disabled:opacity-60">
              {t('signalement.envoyer')}
            </button>
            <button type="button" onClick={() => setOuvert(false)} className="rounded-lg px-3 py-1 text-gray-600 hover:bg-perle">
              {t('commun.annuler')}
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
