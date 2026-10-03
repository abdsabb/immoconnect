import { Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import { formatDate } from '../services/admin'
import { Photo } from './CarteBien'

/** Date, temps de lecture et auteur d'un article. */
export function MetaArticle({ article, className = 'text-xs' }) {
  const { t, i18n } = useTranslation()
  return (
    <p className={`text-gray-500 ${className}`}>
      {formatDate(article.publieLe, i18n.resolvedLanguage)} · {t('blog.lecture', { minutes: article.minutesDeLecture })} · {article.auteur}
    </p>
  )
}

// Carte d'un article du blog : image de couverture, catégorie, titre, chapeau. Toute la carte mène à l'article
// (le lien du titre s'étend sur la carte), et le niveau du titre suit la page, comme pour une carte de bien.
// « aLaUne » : grande carte en deux colonnes, pour le premier article d'une page.
export default function CarteArticle({ article, niveau: Titre = 'h2', aLaUne = false, etiquette }) {
  const { t } = useTranslation()
  const categorie = (
    <span className="absolute left-3 top-3 rounded-full bg-white/95 px-3 py-1 text-xs font-semibold text-nuit shadow">{article.categorie}</span>
  )
  if (aLaUne) {
    return (
      <article className="group relative grid overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm transition-shadow hover:shadow-md md:grid-cols-2">
        <div className="relative overflow-hidden">
          <Photo src={article.imageUrl} alt="" className="h-60 w-full transition-transform duration-300 group-hover:scale-105 md:h-full md:min-h-80" />
          {categorie}
        </div>
        <div className="flex flex-col gap-3 p-6 md:p-8">
          {etiquette && <p className="text-xs font-bold uppercase tracking-widest text-corail">{etiquette}</p>}
          <Titre className="font-titre text-2xl font-bold leading-snug text-nuit md:text-3xl">
            <Link to={`/blog/${article.id}`} className="after:absolute after:inset-0 group-hover:text-turquoise">{article.titre}</Link>
          </Titre>
          <p className="text-gray-600">{article.extrait}</p>
          <MetaArticle article={article} className="mt-auto pt-2 text-xs" />
          <p aria-hidden="true" className="font-titre font-bold text-turquoise">{t('blog.lire')} →</p>
        </div>
      </article>
    )
  }
  return (
    <article className="group relative flex flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm transition-shadow hover:shadow-md">
      <div className="relative overflow-hidden">
        <Photo src={article.imageUrl} alt="" className="h-48 w-full transition-transform duration-300 group-hover:scale-105" />
        {categorie}
      </div>
      <div className="flex flex-1 flex-col gap-2 p-5">
        <Titre className="font-titre font-bold leading-snug text-nuit">
          <Link to={`/blog/${article.id}`} className="after:absolute after:inset-0 group-hover:text-turquoise">{article.titre}</Link>
        </Titre>
        <p className="line-clamp-3 text-sm text-gray-600">{article.extrait}</p>
        <MetaArticle article={article} className="mt-auto pt-2 text-xs" />
      </div>
    </article>
  )
}
