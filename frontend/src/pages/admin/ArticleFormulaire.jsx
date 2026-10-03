import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useForm } from 'react-hook-form'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import {
  chargerArticleAdmin, chargerCategoriesBlog, creerArticle, definirImageArticle, modifierArticle, retirerImageArticle,
} from '../../services/admin'
import { TAILLE_MAX_PHOTO } from '../../services/annonces'
import { BoutonPrincipal, Champ, classeInput, erreursApi } from '../../components/Formulaire'
import { Avis } from './Administration'

// Cas A2 : rédiger ou modifier un article, avec son image de couverture. Un nouvel article est un brouillon ;
// il se publie depuis la liste.
export default function ArticleFormulaire() {
  const { id } = useParams()
  const creation = id === undefined
  const { t } = useTranslation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [avis, setAvis] = useState({})
  // Image choisie sur le poste (envoyée à l'enregistrement), ou demande de retrait de l'image actuelle
  const [fichier, setFichier] = useState(null)
  const [retirer, setRetirer] = useState(false)
  const apercu = useMemo(() => (fichier ? URL.createObjectURL(fichier) : null), [fichier])
  useEffect(() => () => { if (apercu) URL.revokeObjectURL(apercu) }, [apercu])

  const categories = useQuery({ queryKey: ['blog-categories'], queryFn: chargerCategoriesBlog, staleTime: Infinity })
  const article = useQuery({ queryKey: ['admin', 'article', id], queryFn: () => chargerArticleAdmin(id), enabled: !creation })
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm({
    defaultValues: { titre: '', categorieId: '', contenu: '' },
  })

  useEffect(() => {
    if (article.data && categories.data) {
      reset({ titre: article.data.titre, categorieId: String(article.data.categorieId), contenu: article.data.contenu })
    }
  }, [article.data, categories.data, reset])

  const choisirImage = (e) => {
    const choisi = e.target.files[0] ?? null
    setAvis({})
    // Contrôles de confort : le serveur refait les siens sur le contenu réel du fichier
    if (choisi && !['image/jpeg', 'image/png'].includes(choisi.type)) {
      e.target.value = ''
      return setAvis({ erreur: t('annonce.photoFormat') })
    }
    if (choisi && choisi.size > TAILLE_MAX_PHOTO) {
      e.target.value = ''
      return setAvis({ erreur: t('annonce.photoTropLourde') })
    }
    setFichier(choisi)
    if (choisi) setRetirer(false)
  }

  const enregistrer = async (valeurs) => {
    setAvis({})
    const requete = { titre: valeurs.titre, contenu: valeurs.contenu, categorieId: Number(valeurs.categorieId) }
    let enregistre
    try {
      enregistre = creation ? await creerArticle(requete) : await modifierArticle(id, requete)
    } catch (e) {
      const { message, champs } = erreursApi(e, t)
      Object.entries(champs).forEach(([champ, msg]) => setError(champ, { message: msg }))
      return setAvis({ erreur: Object.keys(champs).length ? t('annonce.corriger') : message })
    }
    const rafraichir = () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'articles'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'article', String(enregistre.id)] })
      queryClient.invalidateQueries({ queryKey: ['articles'] })
      queryClient.invalidateQueries({ queryKey: ['article', String(enregistre.id)] })
    }
    try {
      if (fichier) await definirImageArticle(enregistre.id, fichier)
      else if (retirer) await retirerImageArticle(enregistre.id)
    } catch (e) {
      // Le texte est enregistré : l'administrateur reste sur l'article pour choisir une autre image
      rafraichir()
      setFichier(null)
      if (creation) navigate(`/admin/articles/${enregistre.id}`, { replace: true })
      return setAvis({ erreur: t('admin.article.imageErreur', { message: erreursApi(e, t).champs.fichier ?? erreursApi(e, t).message }) })
    }
    rafraichir()
    navigate('/admin/articles')
  }

  if (!creation && article.isPending) return <p className="text-gray-500">{t('commun.chargement')}</p>
  if (!creation && article.isError) return <Avis erreur={erreursApi(article.error, t).message} />

  const requis = { required: t('auth.requis') }
  const imageActuelle = retirer ? null : article.data?.imageUrl
  const image = apercu ?? imageActuelle
  return (
    <div className="space-y-4">
      <Link to="/admin/articles" className="text-sm text-turquoise hover:underline">‹ {t('admin.article.retour')}</Link>
      <h2 className="text-xl font-bold text-nuit">{t(creation ? 'admin.article.nouveau' : 'admin.article.modifier')}</h2>
      <Avis {...avis} />
      <form onSubmit={handleSubmit(enregistrer)} noValidate className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
        <div className="grid sm:grid-cols-3 gap-4">
          <div className="sm:col-span-2">
            <Champ label={t('annonce.titre')} erreur={errors.titre?.message}>
              <input maxLength="150" className={classeInput(errors.titre)} {...register('titre', requis)} />
            </Champ>
          </div>
          <Champ label={t('annonce.categorie')} erreur={errors.categorieId?.message}>
            <select className={classeInput(errors.categorieId)} {...register('categorieId', requis)}>
              <option value="">—</option>
              {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.nom}</option>)}
            </select>
          </Champ>
        </div>

        <div className="grid gap-4 sm:grid-cols-3 sm:items-start">
          {image
            ? <img src={image} alt="" className="aspect-[16/9] w-full rounded-lg object-cover" />
            : <div className="flex aspect-[16/9] w-full items-center justify-center rounded-lg border border-dashed border-gray-300 text-sm text-gray-500">{t('admin.article.imageAucune')}</div>}
          <div className="space-y-2 sm:col-span-2">
            <Champ label={t('admin.article.image')}>
              <input type="file" accept="image/jpeg,image/png" className="w-full text-sm" onChange={choisirImage} />
            </Champ>
            <p className="text-xs text-gray-500">{t('admin.article.imageAide')}</p>
            {article.data?.imageUrl && !fichier && (
              <button type="button" onClick={() => setRetirer(!retirer)} className="text-sm font-semibold text-erreur hover:underline">
                {t(retirer ? 'admin.article.imageAnnuler' : 'admin.article.imageRetirer')}
              </button>
            )}
          </div>
        </div>

        <Champ label={t('admin.article.contenu')} erreur={errors.contenu?.message}>
          <textarea rows="14" maxLength="20000" className={classeInput(errors.contenu)} {...register('contenu', requis)} />
        </Champ>
        <p className="text-xs text-gray-500">{t('admin.article.consigne')}</p>
        <BoutonPrincipal chargement={isSubmitting}>{t('profil.enregistrer')}</BoutonPrincipal>
      </form>
    </div>
  )
}
