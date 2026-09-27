import { useLocation, useNavigate } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { useFavoris } from '../services/favoris'

// Cas M1 : cœur d'ajout aux favoris. Un visiteur est conduit à la connexion puis ramené sur la page.
export default function BoutonFavori({ bien, className = '' }) {
  const { t } = useTranslation()
  const { estConnecte } = useAuth()
  const { estMembre, estFavori, basculer, enCours } = useFavoris()
  const navigate = useNavigate()
  const location = useLocation()

  const bienId = bien.id
  const actif = estFavori(bienId)
  if (estConnecte && !estMembre) return null
  // RA5 : un bien qui n'est plus disponible ne s'ajoute plus, mais un favori existant se retire toujours
  if (bien.statut !== 'disponible' && !actif) return null

  const cliquer = (e) => {
    e.preventDefault()
    if (!estConnecte) navigate('/connexion', { state: { from: location.pathname } })
    else basculer(bienId)
  }

  return (
    <button type="button" onClick={cliquer} disabled={enCours} aria-pressed={actif}
      aria-label={t(actif ? 'favori.retirer' : 'favori.ajouter')} title={t(actif ? 'favori.retirer' : 'favori.ajouter')}
      className={`inline-flex items-center justify-center rounded-full bg-white shadow w-10 h-10 disabled:opacity-60 ${actif ? 'text-corail' : 'text-gray-400 hover:text-corail'} ${className}`}>
      <svg width="22" height="22" viewBox="0 0 24 24" fill={actif ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2" aria-hidden="true">
        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </button>
  )
}
