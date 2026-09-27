import { Link, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerArticle, formatDate } from '../services/admin'

// Page d'un article du blog (gabarit « article »). Le contenu est affiché comme du texte :
// React échappe tout ce qu'il affiche, aucune balise saisie par un rédacteur n'est interprétée.
export default function Article() {
  const { id } = useParams()
  const { t, i18n } = useTranslation()
  const { data: article, isPending, isError } = useQuery({ queryKey: ['article', id], queryFn: () => chargerArticle(id) })

  if (isPending) return <p className="mx-auto max-w-3xl px-4 py-10 text-gray-500">{t('commun.chargement')}</p>
  if (isError) {
    return (
      <section className="mx-auto max-w-3xl px-4 py-10">
        <p role="alert" className="text-erreur">{t('blog.introuvable')}</p>
        <Link to="/blog" className="mt-4 inline-block text-turquoise underline">{t('blog.retour')}</Link>
      </section>
    )
  }
  return (
    <article className="mx-auto max-w-3xl px-4 py-10">
      <nav aria-label="Fil d'Ariane" className="text-sm text-gray-500">
        <Link to="/blog" className="hover:text-turquoise">{t('blog.titre')}</Link> › <span className="text-nuit">{article.categorie}</span>
      </nav>
      <h1 className="mt-3 text-3xl font-bold text-nuit">{article.titre}</h1>
      <p className="mt-1 text-sm text-gray-500">{formatDate(article.publieLe, i18n.resolvedLanguage)} · {article.auteur}</p>
      <div className="mt-6 whitespace-pre-line leading-relaxed text-gray-800">{article.contenu}</div>
      <Link to="/blog" className="mt-8 inline-block text-turquoise font-semibold underline">{t('blog.retour')}</Link>
    </article>
  )
}
