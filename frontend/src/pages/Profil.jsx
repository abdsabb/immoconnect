import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { api } from '../services/api'
import { Champ, BoutonPrincipal, classeInput, erreursApi } from '../components/Formulaire'

// Espace membre : profil (M6), mot de passe, désinscription RGPD (M7 — soft delete RA11)
export default function Profil() {
  const { t, i18n } = useTranslation()
  const { utilisateur, mettreAJour, deconnecter } = useAuth()
  const navigate = useNavigate()
  const [message, setMessage] = useState(null)
  const [erreur, setErreur] = useState(null)

  const profil = useForm({ defaultValues: { nom: utilisateur?.nom, prenom: utilisateur?.prenom, telephone: '', langue: utilisateur?.langue ?? 'fr' } })
  const mdp = useForm()

  const enregistrerProfil = async (valeurs) => {
    setMessage(null); setErreur(null)
    try {
      const { data } = await api.patch('/auth/me', valeurs)
      mettreAJour(data)
      i18n.changeLanguage(data.langue)
      setMessage(t('profil.enregistre'))
    } catch (e) {
      const { message: m, champs } = erreursApi(e, t)
      Object.entries(champs).forEach(([c, msg]) => profil.setError(c, { message: msg }))
      if (!Object.keys(champs).length) setErreur(m)
    }
  }

  const changerMotDePasse = async (valeurs) => {
    setMessage(null); setErreur(null)
    try {
      await api.put('/auth/me/mot-de-passe', valeurs)
      mdp.reset()
      setMessage(t('profil.motDePasseChange'))
    } catch (e) {
      setErreur(erreursApi(e, t).message)
    }
  }

  const seDesinscrire = async () => {
    if (!window.confirm(t('profil.confirmerDesinscription'))) return
    try {
      await api.delete('/auth/me')
      deconnecter()
      navigate('/', { replace: true, state: { desinscrit: true } })
    } catch (e) {
      setErreur(erreursApi(e, t).message)
    }
  }

  if (!utilisateur) return null
  return (
    <section className="mx-auto max-w-3xl px-4 py-12 space-y-10">
      <header>
        <h1 className="text-3xl font-bold text-nuit">{t('profil.titre', { prenom: utilisateur.prenom })}</h1>
        <p className="text-gray-600">{utilisateur.email} · <span className="uppercase text-xs font-semibold text-turquoise">{utilisateur.role}</span></p>
      </header>

      {message && <p role="status" className="rounded-lg bg-succes/10 text-succes px-4 py-3">{message}</p>}
      {erreur && <p role="alert" className="rounded-lg bg-erreur/10 text-erreur px-4 py-3">{erreur}</p>}

      <form onSubmit={profil.handleSubmit(enregistrerProfil)} noValidate className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
        <h2 className="text-xl font-bold text-nuit">{t('profil.informations')}</h2>
        <div className="grid sm:grid-cols-2 gap-4">
          <Champ label={t('auth.prenom')} erreur={profil.formState.errors.prenom?.message}>
            <input className={classeInput(profil.formState.errors.prenom)} {...profil.register('prenom', { required: t('auth.requis') })} />
          </Champ>
          <Champ label={t('auth.nom')} erreur={profil.formState.errors.nom?.message}>
            <input className={classeInput(profil.formState.errors.nom)} {...profil.register('nom', { required: t('auth.requis') })} />
          </Champ>
          <Champ label={t('auth.telephone')} erreur={profil.formState.errors.telephone?.message}>
            <input type="tel" className={classeInput(profil.formState.errors.telephone)} {...profil.register('telephone')} />
          </Champ>
          <Champ label={t('auth.langue')}>
            <select className={classeInput()} {...profil.register('langue')}>
              <option value="fr">Français</option><option value="nl">Nederlands</option><option value="en">English</option>
            </select>
          </Champ>
        </div>
        <BoutonPrincipal chargement={profil.formState.isSubmitting}>{t('profil.enregistrer')}</BoutonPrincipal>
      </form>

      <form onSubmit={mdp.handleSubmit(changerMotDePasse)} noValidate className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
        <h2 className="text-xl font-bold text-nuit">{t('profil.motDePasse')}</h2>
        <Champ label={t('profil.ancienMotDePasse')} erreur={mdp.formState.errors.ancienMotDePasse?.message}>
          <input type="password" autoComplete="current-password" className={classeInput(mdp.formState.errors.ancienMotDePasse)}
            {...mdp.register('ancienMotDePasse', { required: t('auth.requis') })} />
        </Champ>
        <Champ label={t('profil.nouveauMotDePasse')} erreur={mdp.formState.errors.nouveauMotDePasse?.message}>
          <input type="password" autoComplete="new-password" className={classeInput(mdp.formState.errors.nouveauMotDePasse)}
            {...mdp.register('nouveauMotDePasse', { required: t('auth.requis'), minLength: { value: 8, message: t('auth.motDePasseCourt') } })} />
        </Champ>
        <BoutonPrincipal chargement={mdp.formState.isSubmitting}>{t('profil.changerMotDePasse')}</BoutonPrincipal>
      </form>

      {utilisateur.role === 'membre' && (
        <div className="border border-erreur/40 rounded-xl p-6 space-y-3">
          <h2 className="text-xl font-bold text-erreur">{t('profil.desinscription')}</h2>
          <p className="text-sm text-gray-700">{t('profil.desinscriptionExplication')}</p>
          <button type="button" onClick={seDesinscrire}
            className="bg-erreur hover:bg-erreur/90 text-white font-titre font-bold rounded-lg px-4 py-2">
            {t('profil.supprimerCompte')}
          </button>
        </div>
      )}
    </section>
  )
}
