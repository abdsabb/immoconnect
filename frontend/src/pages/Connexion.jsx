import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { api } from '../services/api'
import { Champ, BoutonPrincipal, classeInput, erreursApi } from '../components/Formulaire'

// Cas M9 : se connecter — maquette « Connexion » du prototype (erreurs de formulaire en rouge).
// Deuxième étape quand un code est attendu (double facteur, livrable 16 §2.2).
export default function Connexion() {
  const { t } = useTranslation()
  const { connecter, validerCode } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [erreurApi, setErreurApi] = useState(null)
  const [defi, setDefi] = useState(null)
  const [email, setEmail] = useState('')
  const [nonActive, setNonActive] = useState(false)
  const [renvoye, setRenvoye] = useState(false)
  const identifiants = useForm()
  const code = useForm()

  const destination = location.state?.from ?? '/profil'

  const soumettre = async (valeurs) => {
    setErreurApi(null); setNonActive(false)
    try {
      const resultat = await connecter(valeurs)
      if (resultat.doubleFacteur) {
        setEmail(valeurs.email)
        setDefi(resultat.defi)
        return
      }
      navigate(destination, { replace: true })
    } catch (e) {
      const { message, type } = erreursApi(e, t)
      if (type === 'compte-non-active') setNonActive(true)
      setErreurApi(message)
      setEmail(valeurs.email)
    }
  }

  const soumettreCode = async (valeurs) => {
    setErreurApi(null)
    try {
      await validerCode(defi, valeurs.code)
      navigate(destination, { replace: true })
    } catch (e) {
      const { message, type } = erreursApi(e, t)
      setErreurApi(message)
      // Défi perdu (expiré, trop d'essais) : retour à la première étape
      if (type === 'lien-invalide') setDefi(null)
    }
  }

  const renvoyerActivation = async () => {
    try {
      await api.post('/auth/activation/renvoi', { email })
      setRenvoye(true)
    } catch (e) {
      setErreurApi(erreursApi(e, t).message)
    }
  }

  // Les deux étapes ont la même structure : sans clé, React garderait le champ e-mail (et sa valeur)
  // comme champ du code
  if (defi) {
    return (
      <section key="code" className="mx-auto max-w-md px-4 py-12">
        <h1 className="text-3xl font-bold text-nuit">{t('auth.codeTitre')}</h1>
        <p className="mt-2 text-gray-600">{t('auth.codeSousTitre', { email })}</p>
        <form onSubmit={code.handleSubmit(soumettreCode)} noValidate className="mt-6 space-y-4">
          <Champ label={t('auth.code')} erreur={code.formState.errors.code?.message}>
            <input inputMode="numeric" autoComplete="one-time-code" maxLength="6" className={`${classeInput(code.formState.errors.code)} text-center text-2xl tracking-[0.5em]`}
              {...code.register('code', { required: t('auth.requis'), pattern: { value: /^\s*\d{6}\s*$/, message: t('auth.codeFormat') } })} />
          </Champ>
          {erreurApi && <p role="alert" className="text-erreur text-sm">{erreurApi}</p>}
          <BoutonPrincipal chargement={code.formState.isSubmitting}>{t('auth.valider')}</BoutonPrincipal>
        </form>
        <button type="button" onClick={() => { setDefi(null); setErreurApi(null) }} className="mt-6 text-sm text-turquoise underline">
          {t('auth.recommencer')}
        </button>
      </section>
    )
  }

  return (
    <section key="identifiants" className="mx-auto max-w-md px-4 py-12">
      <h1 className="text-3xl font-bold text-nuit">{t('auth.connexionTitre')}</h1>
      <p className="mt-2 text-gray-600">{t('auth.connexionSousTitre')}</p>
      <form onSubmit={identifiants.handleSubmit(soumettre)} noValidate className="mt-6 space-y-4">
        <Champ label={t('auth.email')} erreur={identifiants.formState.errors.email?.message}>
          <input type="email" autoComplete="email" className={classeInput(identifiants.formState.errors.email)}
            {...identifiants.register('email', { required: t('auth.requis') })} />
        </Champ>
        <Champ label={t('auth.motDePasse')} erreur={identifiants.formState.errors.motDePasse?.message}>
          <input type="password" autoComplete="current-password" className={classeInput(identifiants.formState.errors.motDePasse)}
            {...identifiants.register('motDePasse', { required: t('auth.requis') })} />
        </Champ>
        {erreurApi && <p role="alert" className="text-erreur text-sm">{erreurApi}</p>}
        {nonActive && !renvoye && (
          <button type="button" onClick={renvoyerActivation} className="text-sm text-turquoise underline">{t('auth.renvoyerActivation')}</button>
        )}
        {renvoye && <p role="status" className="text-sm text-succes">{t('auth.activationRenvoyee')}</p>}
        <BoutonPrincipal chargement={identifiants.formState.isSubmitting}>{t('nav.connexion')}</BoutonPrincipal>
      </form>
      <p className="mt-4 text-sm">
        <Link to="/mot-de-passe-oublie" className="text-turquoise underline">{t('auth.motDePasseOublie')}</Link>
      </p>
      <p className="mt-6 text-sm text-gray-600">
        {t('auth.pasDeCompte')} <Link to="/inscription" className="text-turquoise font-semibold underline">{t('nav.inscription')}</Link>
      </p>
    </section>
  )
}
