import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { useConfiguration } from '../services/configuration'
import { Champ, BoutonPrincipal, classeInput, erreursApi, reglesMotDePasse } from '../components/Formulaire'


// Cas V7 : s'inscrire en tant que membre — validation côté client miroir du dictionnaire de données,
// la validation faisant autorité restant celle du serveur (422 avec le détail par champ).
// Les conditions générales sont acceptées explicitement ; le consentement aux communications est un choix.
export default function Inscription() {
  const { t, i18n } = useTranslation()
  const { inscrire } = useAuth()
  const navigate = useNavigate()
  const [erreurApi, setErreurApi] = useState(null)
  const [enAttente, setEnAttente] = useState(null)
  const configuration = useConfiguration()
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm({
    defaultValues: { langue: i18n.resolvedLanguage ?? 'fr', cguAcceptees: false, consentementCommunications: false },
  })

  const soumettre = async (valeurs) => {
    setErreurApi(null)
    try {
      const resultat = await inscrire(valeurs)
      if (resultat.activationRequise) {
        setEnAttente(valeurs.email)
        return
      }
      navigate('/profil', { replace: true })
    } catch (e) {
      const { message, champs } = erreursApi(e, t)
      Object.entries(champs).forEach(([champ, msg]) => setError(champ, { message: msg }))
      if (Object.keys(champs).length === 0) setErreurApi(message)
    }
  }

  if (enAttente) {
    const boite = configuration.data?.boiteDeDemonstration
    return (
      <section className="mx-auto max-w-md px-4 py-12">
        <h1 className="text-3xl font-bold text-nuit">{t('auth.verifiezTitre')}</h1>
        <p role="status" className="mt-6 rounded-lg bg-succes/10 px-4 py-3 text-succes">{t('auth.verifiezTexte', { email: enAttente })}</p>
        {boite && (
          <p className="mt-4 rounded-lg border border-ambre bg-ambre/10 px-4 py-3 text-sm text-gray-800">
            {t('auth.verifiezDemo')}{' '}
            <a href={boite} target="_blank" rel="noreferrer" className="font-semibold text-turquoise underline">{t('auth.verifiezBoite')}</a>
          </p>
        )}
        <p className="mt-6 text-sm text-gray-600"><Link to="/connexion" className="text-turquoise underline">{t('auth.retourConnexion')}</Link></p>
      </section>
    )
  }

  return (
    <section className="mx-auto max-w-md px-4 py-12">
      <h1 className="text-3xl font-bold text-nuit">{t('auth.inscriptionTitre')}</h1>
      <p className="mt-2 text-gray-600">{t('auth.inscriptionSousTitre')}</p>
      <form onSubmit={handleSubmit(soumettre)} noValidate className="mt-6 space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <Champ label={t('auth.prenom')} erreur={errors.prenom?.message}>
            <input autoComplete="given-name" className={classeInput(errors.prenom)} {...register('prenom', { required: t('auth.requis') })} />
          </Champ>
          <Champ label={t('auth.nom')} erreur={errors.nom?.message}>
            <input autoComplete="family-name" className={classeInput(errors.nom)} {...register('nom', { required: t('auth.requis') })} />
          </Champ>
        </div>
        <Champ label={t('auth.email')} erreur={errors.email?.message}>
          <input type="email" autoComplete="email" className={classeInput(errors.email)}
            {...register('email', { required: t('auth.requis'), pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: t('auth.emailInvalide') } })} />
        </Champ>
        <Champ label={t('auth.telephone')} erreur={errors.telephone?.message}>
          <input type="tel" autoComplete="tel" placeholder="+32 4xx xx xx xx" className={classeInput(errors.telephone)} {...register('telephone')} />
        </Champ>
        <Champ label={t('auth.motDePasse')} erreur={errors.motDePasse?.message}>
          <input type="password" autoComplete="new-password" className={classeInput(errors.motDePasse)} {...register('motDePasse', reglesMotDePasse(t))} />
        </Champ>
        <p className="-mt-2 text-xs text-gray-500">{t('auth.motDePasseRegles')}</p>
        <Champ label={t('auth.langue')}>
          <select className={classeInput()} {...register('langue')}>
            <option value="fr">Français</option><option value="nl">Nederlands</option><option value="en">English</option>
          </select>
        </Champ>
        <label className="flex items-start gap-3 text-sm text-gray-700">
          <input type="checkbox" className="mt-1" {...register('cguAcceptees', { required: t('auth.cguRequises') })} />
          <span>
            {t('auth.cguAvant')}{' '}
            <Link to="/conditions" className="font-semibold text-turquoise underline">{t('auth.cgu')}</Link>{' '}
            {t('auth.cguEntre')}{' '}
            <Link to="/confidentialite" className="font-semibold text-turquoise underline">{t('auth.cguConfidentialite')}</Link>.
          </span>
        </label>
        {errors.cguAcceptees && <p role="alert" className="-mt-2 text-sm text-erreur">{errors.cguAcceptees.message}</p>}
        <label className="flex items-start gap-3 text-sm text-gray-700">
          <input type="checkbox" className="mt-1" {...register('consentementCommunications')} />
          <span>{t('auth.consentement')}</span>
        </label>
        {erreurApi && <p role="alert" className="text-erreur text-sm">{erreurApi}</p>}
        <BoutonPrincipal chargement={isSubmitting}>{t('nav.inscription')}</BoutonPrincipal>
      </form>
      <p className="mt-6 text-sm text-gray-600">
        {t('auth.dejaCompte')} <Link to="/connexion" className="text-turquoise font-semibold underline">{t('nav.connexion')}</Link>
      </p>
    </section>
  )
}
