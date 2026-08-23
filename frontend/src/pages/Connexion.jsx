import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { Champ, BoutonPrincipal, classeInput, erreursApi } from '../components/Formulaire'

// Cas M9 : se connecter — maquette « Connexion » du prototype (erreurs de formulaire en rouge)
export default function Connexion() {
  const { t } = useTranslation()
  const { connecter } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [erreurApi, setErreurApi] = useState(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm()

  const soumettre = async (valeurs) => {
    setErreurApi(null)
    try {
      await connecter(valeurs)
      navigate(location.state?.from ?? '/profil', { replace: true })
    } catch (e) {
      setErreurApi(erreursApi(e, t).message)
    }
  }

  return (
    <section className="mx-auto max-w-md px-4 py-12">
      <h1 className="text-3xl font-bold text-nuit">{t('auth.connexionTitre')}</h1>
      <p className="mt-2 text-gray-600">{t('auth.connexionSousTitre')}</p>
      <form onSubmit={handleSubmit(soumettre)} noValidate className="mt-6 space-y-4">
        <Champ label={t('auth.email')} erreur={errors.email?.message}>
          <input type="email" autoComplete="email" className={classeInput(errors.email)}
            {...register('email', { required: t('auth.requis') })} />
        </Champ>
        <Champ label={t('auth.motDePasse')} erreur={errors.motDePasse?.message}>
          <input type="password" autoComplete="current-password" className={classeInput(errors.motDePasse)}
            {...register('motDePasse', { required: t('auth.requis') })} />
        </Champ>
        {erreurApi && <p role="alert" className="text-erreur text-sm">{erreurApi}</p>}
        <BoutonPrincipal chargement={isSubmitting}>{t('nav.connexion')}</BoutonPrincipal>
      </form>
      <p className="mt-6 text-sm text-gray-600">
        {t('auth.pasDeCompte')} <Link to="/inscription" className="text-turquoise font-semibold underline">{t('nav.inscription')}</Link>
      </p>
    </section>
  )
}
