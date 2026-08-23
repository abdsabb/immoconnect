import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../services/api'

/**
 * Contexte d'authentification (livrable 16, §2.1) :
 * le jeton JWT est conservé UNIQUEMENT en mémoire (état React), jamais en localStorage —
 * un script injecté ne peut donc pas le lire. Il est transmis dans l'en-tête Authorization.
 * Conséquence assumée en version alpha : un rechargement de page demande une nouvelle connexion
 * (le jeton de rafraîchissement en cookie httpOnly est prévu pour la version bêta).
 */
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [jeton, setJeton] = useState(null)
  const [utilisateur, setUtilisateur] = useState(null)
  const jetonRef = useRef(null)

  // L'intercepteur lit le jeton courant sans se réabonner à chaque rendu
  useEffect(() => {
    jetonRef.current = jeton
  }, [jeton])

  useEffect(() => {
    const id = api.interceptors.request.use((config) => {
      if (jetonRef.current) config.headers.Authorization = `Bearer ${jetonRef.current}`
      return config
    })
    return () => api.interceptors.request.eject(id)
  }, [])

  const appliquer = useCallback((reponse) => {
    setJeton(reponse.jeton)
    setUtilisateur(reponse.utilisateur)
    return reponse.utilisateur
  }, [])

  const connecter = useCallback(async (identifiants) => {
    const { data } = await api.post('/auth/login', identifiants)
    return appliquer(data)
  }, [appliquer])

  const inscrire = useCallback(async (formulaire) => {
    const { data } = await api.post('/auth/register', formulaire)
    return appliquer(data)
  }, [appliquer])

  const deconnecter = useCallback(() => {
    // Sans état côté serveur : se déconnecter = oublier le jeton (livrable 16)
    setJeton(null)
    setUtilisateur(null)
  }, [])

  const mettreAJour = useCallback((u) => setUtilisateur(u), [])

  const valeur = useMemo(
    () => ({ jeton, utilisateur, estConnecte: Boolean(jeton), connecter, inscrire, deconnecter, mettreAJour }),
    [jeton, utilisateur, connecter, inscrire, deconnecter, mettreAJour],
  )
  return <AuthContext.Provider value={valeur}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth doit être utilisé dans un AuthProvider')
  return ctx
}
