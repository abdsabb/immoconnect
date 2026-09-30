import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { api } from '../services/api'
import { Champ, BoutonPrincipal, classeInput, erreursApi, reglesMotDePasse } from '../components/Formulaire'

// Cas M10 : récupérer son mot de passe. Première étape : demander le lien ; seconde, avec ?jeton= : choisir le nouveau.
export default function MotDePasseOublie() {
  const [adresse] = useSearchParams()
  const jeton = adresse.get('jeton')
  return jeton ? <NouveauMotDePasse jeton={jeton} /> : <DemandeDeLien />
}

function DemandeDeLien() {
  const { t } = useTranslation()
  const [envoye, setEnvoye] = useState(false)
  const [erreur, setErreur] = useState(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm()

  const soumettre = async ({ email }) => {
    setErreur(null)
    try {
      await api.post('/auth/mot-de-passe-oublie', { email })
      setEnvoye(true)
    } catch (e) {
      setErreur(erreursApi(e, t).message)
    }
  }

  return (
    <section className="mx-auto max-w-md px-4 py-12">
      <h1 className="text-3xl font-bold text-nuit">{t('auth.oubliTitre')}</h1>
      <p className="mt-2 text-gray-600">{t('auth.oubliSousTitre')}</p>
      {envoye ? (
        <p role="status" className="mt-6 rounded-lg bg-succes/10 px-4 py-3 text-succes">{t('auth.oubliEnvoye')}</p>
      ) : (
        <form onSubmit={handleSubmit(soumettre)} noValidate className="mt-6 space-y-4">
          <Champ label={t('auth.email')} erreur={errors.email?.message}>
            <input type="email" autoComplete="email" className={classeInput(errors.email)} {...register('email', { required: t('auth.requis') })} />
          </Champ>
          {erreur && <p role="alert" className="text-erreur text-sm">{erreur}</p>}
          <BoutonPrincipal chargement={isSubmitting}>{t('auth.oubliBouton')}</BoutonPrincipal>
        </form>
      )}
      <p className="mt-6 text-sm text-gray-600"><Link to="/connexion" className="text-turquoise underline">{t('auth.retourConnexion')}</Link></p>
    </section>
  )
}

function NouveauMotDePasse({ jeton }) {
  const { t } = useTranslation()
  const { deconnecter } = useAuth()
  const [fait, setFait] = useState(false)
  const [erreur, setErreur] = useState(null)
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm()

  const soumettre = async ({ nouveauMotDePasse }) => {
    setErreur(null)
    try {
      await api.post('/auth/reinitialisation', { jeton, nouveauMotDePasse })
      await deconnecter()
      setFait(true)
    } catch (e) {
      const { message, champs } = erreursApi(e, t)
      Object.entries(champs).forEach(([champ, msg]) => setError(champ, { message: msg }))
      if (Object.keys(champs).length === 0) setErreur(message)
    }
  }

  return (
    <section className="mx-auto max-w-md px-4 py-12">
      <h1 className="text-3xl font-bold text-nuit">{t('auth.reinitialisationTitre')}</h1>
      {fait ? (
        <>
          <p role="status" className="mt-6 rounded-lg bg-succes/10 px-4 py-3 text-succes">{t('auth.reinitialisationFaite')}</p>
          <p className="mt-6 text-sm"><Link to="/connexion" className="text-turquoise font-semibold underline">{t('nav.connexion')}</Link></p>
        </>
      ) : (
        <form onSubmit={handleSubmit(soumettre)} noValidate className="mt-6 space-y-4">
          <Champ label={t('profil.nouveauMotDePasse')} erreur={errors.nouveauMotDePasse?.message}>
            <input type="password" autoComplete="new-password" className={classeInput(errors.nouveauMotDePasse)}
              {...register('nouveauMotDePasse', reglesMotDePasse(t))} />
          </Champ>
          <p className="text-xs text-gray-500">{t('auth.motDePasseRegles')}</p>
          {erreur && <p role="alert" className="text-erreur text-sm">{erreur}</p>}
          <BoutonPrincipal chargement={isSubmitting}>{t('auth.reinitialisationBouton')}</BoutonPrincipal>
        </form>
      )}
    </section>
  )
}
