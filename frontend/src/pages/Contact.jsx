import { useState } from 'react'
import { Link } from 'react-router'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { envoyerContact } from '../services/configuration'
import { conversion } from '../services/mesure'
import { useTitrePage } from '../services/titre'
import { BoutonPrincipal, Champ, classeInput, erreursApi } from '../components/Formulaire'

// Page « Contact » (quatrième entrée de la navigation, charte graphique) : un formulaire. Le message est
// enregistré, transmis par e-mail à l'agence et suivi dans le back-office.
export default function Contact() {
  const { t } = useTranslation()
  const { utilisateur } = useAuth()
  const [envoye, setEnvoye] = useState(false)
  const [erreur, setErreur] = useState(null)
  useTitrePage(t('nav.contact'))
  // Une personne connectée n'a pas à retaper son nom ni son adresse
  const { register, handleSubmit, setError, reset, formState: { errors, isSubmitting } } = useForm({
    defaultValues: { nom: utilisateur ? `${utilisateur.prenom} ${utilisateur.nom}` : '', email: utilisateur?.email ?? '', telephone: '', sujet: '', message: '', site: '' },
  })

  const envoyer = async (valeurs) => {
    setErreur(null)
    try {
      await envoyerContact(valeurs)
      conversion('contact')
      setEnvoye(true)
      reset()
    } catch (e) {
      const { message, champs } = erreursApi(e, t)
      Object.entries(champs).forEach(([champ, texte]) => setError(champ, { message: texte }))
      setErreur(Object.keys(champs).length ? t('annonce.corriger') : message)
    }
  }
  const champ = (nom, regles) => ({ className: classeInput(errors[nom]), ...register(nom, regles) })
  const requis = { required: t('auth.requis') }

  return (
    <section className="mx-auto max-w-2xl px-4 py-10">
      <nav aria-label="Fil d'Ariane" className="text-sm text-gray-500">
        <Link to="/" className="hover:text-turquoise">{t('nav.accueil')}</Link> › <span className="text-nuit">{t('nav.contact')}</span>
      </nav>
      <h1 className="mt-3 text-3xl font-bold text-nuit">{t('contact.titre')}</h1>
      <p className="mt-1 text-gray-600">{t('contact.sousTitre')}</p>

      {envoye ? (
        <div className="mt-8 space-y-4">
          <p role="status" className="rounded-lg bg-succes/10 px-4 py-3 text-succes">{t('contact.envoye')}</p>
          <button type="button" onClick={() => setEnvoye(false)} className="text-turquoise font-semibold underline">{t('contact.autre')}</button>
        </div>
      ) : (
        <form onSubmit={handleSubmit(envoyer)} noValidate className="mt-8 space-y-4 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <div className="grid gap-4 sm:grid-cols-2">
            <Champ label={t('contact.nom')} erreur={errors.nom?.message}>
              <input autoComplete="name" maxLength="100" {...champ('nom', requis)} />
            </Champ>
            <Champ label={t('auth.email')} erreur={errors.email?.message}>
              <input type="email" autoComplete="email" maxLength="150"
                {...champ('email', { ...requis, pattern: { value: /^[^@\s]+@[^@\s]+\.[^@\s]+$/, message: t('contact.emailInvalide') } })} />
            </Champ>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Champ label={t('contact.telephone')} erreur={errors.telephone?.message}>
              <input type="tel" autoComplete="tel" maxLength="30" placeholder="+32 4xx xx xx xx" {...champ('telephone')} />
            </Champ>
            <Champ label={t('contact.sujet')} erreur={errors.sujet?.message}>
              <input maxLength="150" {...champ('sujet', requis)} />
            </Champ>
          </div>
          <Champ label={t('contact.message')} erreur={errors.message?.message}>
            <textarea rows="6" maxLength="3000" {...champ('message', requis)} />
          </Champ>
          {/* Champ piège : caché aux personnes et aux lecteurs d'écran, rempli seulement par les robots */}
          <div aria-hidden="true" className="hidden">
            <label>Site web<input tabIndex="-1" autoComplete="off" {...register('site')} /></label>
          </div>
          {erreur && <p role="alert" className="text-sm text-erreur">{erreur}</p>}
          <p className="text-xs text-gray-500">
            {t('contact.donnees')} <Link to="/confidentialite" className="text-turquoise underline">{t('auth.cguConfidentialite')}</Link>.
          </p>
          <BoutonPrincipal chargement={isSubmitting}>{t('contact.envoyer')}</BoutonPrincipal>
        </form>
      )}

      <p className="mt-6 text-sm text-gray-600">
        {t('contact.agentTexte')}{' '}
        <Link to="/a-vendre" className="font-semibold text-turquoise underline">{t('offre.vente')}</Link>{' · '}
        <Link to="/a-louer" className="font-semibold text-turquoise underline">{t('offre.location')}</Link>
      </p>
    </section>
  )
}
