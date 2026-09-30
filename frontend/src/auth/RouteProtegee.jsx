import { Navigate, Outlet, useLocation } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useAuth } from './AuthContext'

// Les actions réservées redirigent vers la connexion puis ramènent à la page d'origine (livrable 10, §6).
// « roles » restreint un espace à certains profils : ce n'est qu'un confort d'affichage, l'API refuse
// de toute façon (403) ce que le rôle ne permet pas.
export default function RouteProtegee({ roles }) {
  const { t } = useTranslation()
  const { estConnecte, utilisateur, initialisation } = useAuth()
  const location = useLocation()
  // Au chargement de la page, la session est peut-être en train d'être rouverte : on attend avant de rediriger
  if (initialisation) return <p className="mx-auto max-w-3xl px-4 py-12 text-gray-500">{t('commun.chargement')}</p>
  if (!estConnecte) return <Navigate to="/connexion" replace state={{ from: location.pathname + location.search }} />
  if (roles && !roles.includes(utilisateur?.role)) {
    return <p role="alert" className="mx-auto max-w-3xl px-4 py-12 text-erreur">{t('erreur.interdit')}</p>
  }
  return <Outlet />
}
