import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './AuthContext'

// Les actions réservées redirigent vers la connexion puis ramènent à la page d'origine (livrable 10, §6)
export default function RouteProtegee() {
  const { estConnecte } = useAuth()
  const location = useLocation()
  return estConnecte ? <Outlet /> : <Navigate to="/connexion" replace state={{ from: location.pathname }} />
}
