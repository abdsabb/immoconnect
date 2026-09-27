import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useForm } from 'react-hook-form'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerArticleAdmin, chargerCategoriesBlog, creerArticle, modifierArticle } from '../../services/admin'
import { BoutonPrincipal, Champ, classeInput, erreursApi } from '../../components/Formulaire'
import { Avis } from './Administration'

// Cas A2 : rédiger ou modifier un article. Un nouvel article est un brouillon ; il se publie depuis la liste.
export default function ArticleFormulaire() {
  const { id } = useParams()
  const creation = id === undefined
  const { t } = useTranslation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [avis, setAvis] = useState({})

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

  const enregistrer = async (valeurs) => {
    setAvis({})
    const requete = { titre: valeurs.titre, contenu: valeurs.contenu, categorieId: Number(valeurs.categorieId) }
    try {
      if (creation) await creerArticle(requete)
      else await modifierArticle(id, requete)
      queryClient.invalidateQueries({ queryKey: ['admin', 'articles'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'article', id] })
      queryClient.invalidateQueries({ queryKey: ['articles'] })
      queryClient.invalidateQueries({ queryKey: ['article', id] })
      navigate('/admin/articles')
    } catch (e) {
      const { message, champs } = erreursApi(e, t)
      Object.entries(champs).forEach(([champ, msg]) => setError(champ, { message: msg }))
      setAvis({ erreur: Object.keys(champs).length ? t('annonce.corriger') : message })
    }
  }

  if (!creation && article.isPending) return <p className="text-gray-500">{t('commun.chargement')}</p>
  if (!creation && article.isError) return <Avis erreur={erreursApi(article.error, t).message} />

  const requis = { required: t('auth.requis') }
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
        <Champ label={t('admin.article.contenu')} erreur={errors.contenu?.message}>
          <textarea rows="14" maxLength="20000" className={classeInput(errors.contenu)} {...register('contenu', requis)} />
        </Champ>
        <p className="text-xs text-gray-500">{t('admin.article.consigne')}</p>
        <BoutonPrincipal chargement={isSubmitting}>{t('profil.enregistrer')}</BoutonPrincipal>
      </form>
    </div>
  )
}
