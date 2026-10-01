import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api } from '../services/api'

/**
 * Contexte d'authentification (livrable 16, §2.1). La session tient en deux jetons :
 * - le jeton d'accès JWT, gardé UNIQUEMENT en mémoire (état React), jamais en localStorage — un script
 *   injecté ne peut donc pas le lire ; il est transmis dans l'en-tête Authorization et expire vite ;
 * - le jeton de rafraîchissement, dans un cookie HttpOnly posé par l'API, que le navigateur envoie seul
 *   à /auth/refresh : il renouvelle le jeton d'accès au démarrage et à chaque expiration.
 * Recharger la page ne déconnecte donc plus.
 */
const AuthContext = createContext(null)

// Les appels d'authentification eux-mêmes ne se rejouent jamais après un 401
const SANS_REPRISE = ['/auth/login', '/auth/login/code', '/auth/refresh', '/auth/logout', '/auth/register', '/auth/activation']

export function AuthProvider({ children }) {
  const [jeton, setJeton] = useState(null)
  const [utilisateur, setUtilisateur] = useState(null)
  const [initialisation, setInitialisation] = useState(true)
  const jetonRef = useRef(null)
  const rafraichissementEnCours = useRef(null)
  const queryClient = useQueryClient()

  const appliquer = useCallback((reponse) => {
    jetonRef.current = reponse.jeton
    setJeton(reponse.jeton)
    setUtilisateur(reponse.utilisateur)
    return reponse.utilisateur
  }, [])

  const oublier = useCallback(() => {
    jetonRef.current = null
    setJeton(null)
    setUtilisateur(null)
  }, [])

  // Un seul rafraîchissement à la fois : les appels concurrents attendent le même résultat
  const rafraichir = useCallback(() => {
    if (!rafraichissementEnCours.current) {
      rafraichissementEnCours.current = api.post('/auth/refresh')
        .then(({ data }) => appliquer(data))
        .catch(() => { oublier(); return null })
        .finally(() => { rafraichissementEnCours.current = null })
    }
    return rafraichissementEnCours.current
  }, [appliquer, oublier])

  // Au démarrage : le cookie de session, s'il existe, rouvre la session sans rien demander
  useEffect(() => {
    rafraichir().finally(() => setInitialisation(false))
  }, [rafraichir])

  useEffect(() => {
    const requete = api.interceptors.request.use((config) => {
      if (jetonRef.current) config.headers.Authorization = `Bearer ${jetonRef.current}`
      return config
    })
    // Jeton d'accès expiré : on le renouvelle une fois et on rejoue la requête
    const reponse = api.interceptors.response.use(undefined, async (erreur) => {
      const config = erreur.config
      const rejouable = erreur.response?.status === 401 && config && !config.rejouee && jetonRef.current
        && !SANS_REPRISE.some((chemin) => config.url?.startsWith(chemin))
      if (!rejouable) throw erreur
      const nouveau = await rafraichir()
      if (!nouveau) throw erreur
      config.rejouee = true
      config.headers.Authorization = `Bearer ${jetonRef.current}`
      return api(config)
    })
    return () => {
      api.interceptors.request.eject(requete)
      api.interceptors.response.eject(reponse)
    }
  }, [rafraichir])

  const ouvrir = useCallback((reponse) => {
    // Les réponses en cache appartiennent à la session précédente : elles ne doivent jamais
    // s'afficher, même un instant, sous un autre compte (rendez-vous, paiements…)
    queryClient.clear()
    return appliquer(reponse)
  }, [appliquer, queryClient])

  /**
   * @return l'utilisateur connecté, ou { codeAttendu, defi } quand un code est attendu. Le nom « codeAttendu » est
   *         choisi pour ne jamais se confondre avec le champ « doubleFacteur » du profil de l'utilisateur.
   */
  const connecter = useCallback(async (identifiants) => {
    const { data, status } = await api.post('/auth/login', identifiants)
    if (status === 202) return { codeAttendu: true, defi: data.defi, expireDans: data.expireDans }
    return ouvrir(data)
  }, [ouvrir])

  const validerCode = useCallback(async (defi, code) => {
    const { data } = await api.post('/auth/login/code', { defi, code })
    return ouvrir(data)
  }, [ouvrir])

  /** @return l'utilisateur, connecté ou en attente d'activation (data.activationRequise) */
  const inscrire = useCallback(async (formulaire) => {
    const { data } = await api.post('/auth/register', formulaire)
    if (data.activationRequise) return { activationRequise: true, utilisateur: data.utilisateur }
    return ouvrir(data)
  }, [ouvrir])

  const activer = useCallback(async (jetonActivation) => {
    const { data } = await api.post('/auth/activation', { jeton: jetonActivation })
    return ouvrir(data)
  }, [ouvrir])

  const deconnecter = useCallback(async () => {
    queryClient.clear()
    oublier()
    // Le cookie de session est fermé côté serveur ; sans réponse, la session locale est de toute façon oubliée
    try { await api.post('/auth/logout') } catch { /* rien à faire */ }
  }, [oublier, queryClient])

  const mettreAJour = useCallback((u) => setUtilisateur(u), [])

  const valeur = useMemo(
    () => ({ jeton, utilisateur, estConnecte: Boolean(jeton), initialisation, connecter, validerCode, inscrire, activer, deconnecter, mettreAJour }),
    [jeton, utilisateur, initialisation, connecter, validerCode, inscrire, activer, deconnecter, mettreAJour],
  )
  return <AuthContext.Provider value={valeur}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth doit être utilisé dans un AuthProvider')
  return ctx
}
