import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useForm } from 'react-hook-form'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import {
  ajouterPhoto, chargerAnnonce, chargerCategories, creerAnnonce, definirCouverture, modifierAnnonce,
  STATUTS, supprimerPhoto, TAILLE_MAX_PHOTO, versRequete,
} from '../services/annonces'
import { BoutonPrincipal, Champ, classeInput, erreursApi } from '../components/Formulaire'
import { Photo } from '../components/CarteBien'
import CarteChoixPosition from '../components/CarteChoixPosition'

const VIDE = {
  categorieId: '', titre: '', description: '', prix: '', superficie: '', nbChambres: '',
  adresse: '', ville: '', codePostal: '', latitude: '', longitude: '', statut: 'archive',
}

// Cas AG1 « Publier une annonce » et AG2 « Modifier une annonce » : le même formulaire sert aux deux.
// Les validations reproduisent celles du serveur, qui reste seul à faire autorité.
export default function AnnonceFormulaire() {
  const { id } = useParams()
  const creation = id === undefined
  const { t } = useTranslation()
  const { utilisateur } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [message, setMessage] = useState(null)
  const [erreur, setErreur] = useState(null)

  const categories = useQuery({ queryKey: ['categories'], queryFn: chargerCategories, staleTime: Infinity })
  const annonce = useQuery({ queryKey: ['annonce', id], queryFn: () => chargerAnnonce(id), enabled: !creation })
  const { register, handleSubmit, reset, setValue, setError, watch, formState: { errors, isSubmitting } } = useForm({ defaultValues: VIDE })

  useEffect(() => {
    if (annonce.data && categories.data) reset({ ...annonce.data, categorieId: String(annonce.data.categorieId) })
  }, [annonce.data, categories.data, reset])

  const rafraichir = (donnees) => {
    queryClient.setQueryData(['annonce', String(donnees.id)], donnees)
    queryClient.invalidateQueries({ queryKey: ['annonces', utilisateur?.id] })
    queryClient.invalidateQueries({ queryKey: ['bien', String(donnees.id)] })
  }

  const enregistrer = async (valeurs) => {
    setMessage(null)
    setErreur(null)
    try {
      if (creation) {
        const creee = await creerAnnonce(versRequete(valeurs))
        rafraichir(creee)
        navigate(`/annonces/${creee.id}`, { replace: true, state: { creee: true } })
      } else {
        rafraichir(await modifierAnnonce(id, versRequete(valeurs)))
        setMessage(t('annonce.enregistree'))
      }
    } catch (e) {
      const { message: texte, champs } = erreursApi(e, t)
      Object.entries(champs).forEach(([champ, msg]) => setError(champ, { message: msg }))
      // Règle métier (RA6, visites prévues) : le détail renvoyé par l'API dit laquelle
      setErreur(Object.keys(champs).length ? t('annonce.corriger') : (e.response?.data?.detail ?? texte))
      // Le statut affiché revient à celui réellement enregistré
      if (annonce.data) setValue('statut', annonce.data.statut)
    }
  }

  if (!creation && annonce.isPending) return <p className="mx-auto max-w-4xl px-4 py-10 text-gray-500">{t('commun.chargement')}</p>
  if (!creation && annonce.isError) {
    return (
      <section className="mx-auto max-w-4xl px-4 py-10">
        <p role="alert" className="text-erreur">{erreursApi(annonce.error, t).message}</p>
        <Link to="/annonces" className="mt-4 inline-block text-turquoise underline">{t('annonce.retour')}</Link>
      </section>
    )
  }

  const champ = (nom, regles) => ({ className: classeInput(errors[nom]), ...register(nom, regles) })
  const requis = { required: t('auth.requis') }
  const nombre = (min, max) => ({
    ...requis,
    min: { value: min, message: t('annonce.minimum', { min }) },
    ...(max !== undefined && { max: { value: max, message: t('annonce.maximum', { max }) } }),
  })

  return (
    <section className="mx-auto max-w-4xl px-4 py-10 space-y-8">
      <header>
        <Link to="/annonces" className="text-sm text-turquoise hover:underline">‹ {t('annonce.retour')}</Link>
        <h1 className="mt-2 text-3xl font-bold text-nuit">{t(creation ? 'annonce.nouvelle' : 'annonce.modifierTitre')}</h1>
        {creation && <p className="text-gray-600">{t('annonce.explicationCreation')}</p>}
      </header>

      {message && <p role="status" className="rounded-lg bg-succes/10 text-succes px-4 py-3">{message}</p>}
      {erreur && <p role="alert" className="rounded-lg bg-erreur/10 text-erreur px-4 py-3">{erreur}</p>}

      <form onSubmit={handleSubmit(enregistrer)} noValidate className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
        <h2 className="text-xl font-bold text-nuit">{t('annonce.description')}</h2>
        <div className="grid sm:grid-cols-3 gap-4">
          <div className="sm:col-span-2">
            <Champ label={t('annonce.titre')} erreur={errors.titre?.message}>
              <input maxLength="150" {...champ('titre', requis)} />
            </Champ>
          </div>
          <Champ label={t('annonce.categorie')} erreur={errors.categorieId?.message}>
            <select {...champ('categorieId', requis)}>
              <option value="">—</option>
              {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.nom}</option>)}
            </select>
          </Champ>
        </div>
        <Champ label={t('bien.description')} erreur={errors.description?.message}>
          <textarea rows="6" maxLength="10000" {...champ('description', requis)} />
        </Champ>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
          <Champ label={t('annonce.prix')} erreur={errors.prix?.message}>
            <input type="number" step="0.01" min="0.01" {...champ('prix', nombre(0.01))} />
          </Champ>
          <Champ label={t('annonce.superficie')} erreur={errors.superficie?.message}>
            <input type="number" step="0.01" min="0.01" {...champ('superficie', nombre(0.01))} />
          </Champ>
          <Champ label={t('bien.chambresLabel')} erreur={errors.nbChambres?.message}>
            <input type="number" step="1" min="0" max="20" {...champ('nbChambres', nombre(0, 20))} />
          </Champ>
        </div>

        <h2 className="pt-4 text-xl font-bold text-nuit">{t('bien.localisation')}</h2>
        <p className="text-sm text-gray-600">{t('annonce.explicationAdresse')}</p>
        <div className="grid sm:grid-cols-4 gap-4">
          <div className="sm:col-span-2">
            <Champ label={t('rdv.adresse')} erreur={errors.adresse?.message}>
              <input maxLength="150" autoComplete="off" {...champ('adresse', requis)} />
            </Champ>
          </div>
          <Champ label={t('annonce.codePostal')} erreur={errors.codePostal?.message}>
            <input inputMode="numeric" maxLength="4"
              {...champ('codePostal', { ...requis, pattern: { value: /^[1-9][0-9]{3}$/, message: t('annonce.codePostalInvalide') } })} />
          </Champ>
          <Champ label={t('annonce.ville')} erreur={errors.ville?.message}>
            <input maxLength="80" {...champ('ville', requis)} />
          </Champ>
        </div>
        <p className="text-sm text-gray-600">{t('annonce.explicationCarte')}</p>
        <CarteChoixPosition latitude={watch('latitude')} longitude={watch('longitude')}
          onChoisir={(latitude, longitude) => {
            setValue('latitude', latitude, { shouldValidate: true, shouldDirty: true })
            setValue('longitude', longitude, { shouldValidate: true, shouldDirty: true })
          }} />
        <div className="grid grid-cols-2 gap-4">
          <Champ label={t('annonce.latitude')} erreur={errors.latitude?.message}>
            <input type="number" step="0.000001" {...champ('latitude', nombre(-90, 90))} />
          </Champ>
          <Champ label={t('annonce.longitude')} erreur={errors.longitude?.message}>
            <input type="number" step="0.000001" {...champ('longitude', nombre(-180, 180))} />
          </Champ>
        </div>

        {!creation && (
          <>
            <h2 className="pt-4 text-xl font-bold text-nuit">{t('annonce.publication')}</h2>
            <Champ label={t('bien.statutLabel')} erreur={errors.statut?.message}>
              <select {...champ('statut')}>
                {STATUTS.map((s) => <option key={s} value={s}>{t(`annonce.statut.${s}`)}</option>)}
              </select>
            </Champ>
            <p className="text-sm text-gray-600">{t('annonce.explicationStatut')}</p>
          </>
        )}

        <BoutonPrincipal chargement={isSubmitting}>{t(creation ? 'annonce.creer' : 'profil.enregistrer')}</BoutonPrincipal>
      </form>

      {!creation && annonce.data && <Galerie annonce={annonce.data} rafraichir={rafraichir} />}
    </section>
  )
}

// Photos de l'annonce : la première est la couverture affichée dans les résultats de recherche
function Galerie({ annonce, rafraichir }) {
  const { t } = useTranslation()
  const [erreur, setErreur] = useState(null)
  const [enCours, setEnCours] = useState(false)
  const [legende, setLegende] = useState('')
  const fichier = useRef(null)

  const executer = async (action) => {
    setErreur(null)
    setEnCours(true)
    try {
      rafraichir(await action())
    } catch (e) {
      const { message, champs } = erreursApi(e, t)
      setErreur(Object.values(champs)[0] ?? e.response?.data?.detail ?? message)
    } finally {
      setEnCours(false)
    }
  }

  const televerser = (e) => {
    e.preventDefault()
    const choisi = fichier.current?.files?.[0]
    if (!choisi) return
    // Contrôles de confort : le serveur refait les siens sur le contenu réel du fichier
    if (!['image/jpeg', 'image/png'].includes(choisi.type)) return setErreur(t('annonce.photoFormat'))
    if (choisi.size > TAILLE_MAX_PHOTO) return setErreur(t('annonce.photoTropLourde'))
    executer(async () => {
      const resultat = await ajouterPhoto(annonce.id, choisi, legende.trim())
      fichier.current.value = ''
      setLegende('')
      return resultat
    })
  }

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
      <h2 className="text-xl font-bold text-nuit">{t('annonce.photos')} ({annonce.photos.length})</h2>
      {annonce.photos.length === 0 && <p className="rounded-lg bg-ambre/20 text-nuit px-4 py-3">{t('annonce.sansPhoto')}</p>}
      {erreur && <p role="alert" className="rounded-lg bg-erreur/10 text-erreur px-4 py-3">{erreur}</p>}

      <ul className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        {annonce.photos.map((p) => (
          <li key={p.id} className="border border-gray-200 rounded-lg overflow-hidden">
            <Photo src={p.url} alt={p.legende ?? annonce.titre} className="w-full h-32" />
            <div className="p-2 space-y-2 text-sm">
              <p className="text-gray-700 truncate">{p.legende ?? '—'}</p>
              {p.ordre === 1
                ? <span className="inline-block text-xs font-semibold px-2 py-1 rounded-full bg-turquoise text-white">{t('annonce.couverture')}</span>
                : (
                  <button type="button" disabled={enCours} onClick={() => executer(() => definirCouverture(annonce.id, p.id))}
                    className="text-turquoise font-semibold hover:underline disabled:opacity-60">
                    {t('annonce.choisirCouverture')}
                  </button>
                )}
              <button type="button" disabled={enCours}
                onClick={() => window.confirm(t('annonce.confirmerSuppressionPhoto')) && executer(() => supprimerPhoto(annonce.id, p.id))}
                className="block text-erreur font-semibold hover:underline disabled:opacity-60">
                {t('annonce.supprimerPhoto')}
              </button>
            </div>
          </li>
        ))}
      </ul>

      <form onSubmit={televerser} className="grid sm:grid-cols-3 gap-4 items-end border-t border-gray-200 pt-4">
        <Champ label={t('annonce.ajouterPhoto')}>
          <input ref={fichier} type="file" accept="image/jpeg,image/png" required className="w-full text-sm" />
        </Champ>
        <Champ label={t('annonce.legende')}>
          <input maxLength="150" value={legende} onChange={(e) => setLegende(e.target.value)} className={classeInput()} />
        </Champ>
        <BoutonPrincipal chargement={enCours}>{t('annonce.televerser')}</BoutonPrincipal>
      </form>
      <p className="text-xs text-gray-500">{t('annonce.photoConsigne')}</p>
    </div>
  )
}
