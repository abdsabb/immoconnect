import { Link, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerArticle, chargerArticles } from '../services/admin'
import { useTitrePage } from '../services/titre'
import CarteArticle, { MetaArticle } from '../components/CarteArticle'
import { Photo } from '../components/CarteBien'
import ContenuArticle from '../components/ContenuArticle'
import Partage from '../components/Partage'

// Page d'un article du blog (gabarit « article ») : image de couverture, texte mis en forme, partage,
// puis trois autres articles de la même catégorie.
export default function Article() {
  const { id } = useParams()
  const { t } = useTranslation()
  const { data: article, isPending, isError } = useQuery({ queryKey: ['article', id], queryFn: () => chargerArticle(id) })
  const voisins = useQuery({
    queryKey: ['articles', article?.categorieId, 'voisins'],
    queryFn: () => chargerArticles({ categorieId: article.categorieId, taille: 4 }),
    enabled: !!article,
  })
  useTitrePage(article?.titre)

  if (isPending) return <p className="mx-auto max-w-3xl px-4 py-10 text-gray-500">{t('commun.chargement')}</p>
  if (isError) {
    return (
      <section className="mx-auto max-w-3xl px-4 py-10">
        <p role="alert" className="text-erreur">{t('blog.introuvable')}</p>
        <Link to="/blog" className="mt-4 inline-block text-turquoise underline">{t('blog.retour')}</Link>
      </section>
    )
  }
  const aLireAussi = (voisins.data?.contenu ?? []).filter((a) => a.id !== article.id).slice(0, 3)
  return (
    <>
      <article className="mx-auto max-w-3xl px-4 py-10">
        <nav aria-label="Fil d'Ariane" className="text-sm text-gray-500">
          <Link to="/blog" className="hover:text-turquoise">{t('blog.titre')}</Link> › <span className="text-nuit">{article.categorie}</span>
        </nav>
        <p className="mt-6 inline-block rounded-full bg-turquoise/10 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-turquoise">{article.categorie}</p>
        <h1 className="mt-3 text-3xl font-bold leading-tight text-nuit md:text-4xl">{article.titre}</h1>
        <MetaArticle article={article} className="mt-3 text-sm" />
        {article.imageUrl && <Photo src={article.imageUrl} alt="" className="mt-6 aspect-[16/9] w-full rounded-2xl" />}
        <ContenuArticle contenu={article.contenu} className="mt-8" />
        <Partage titre={article.titre} className="mt-10 border-t border-gray-200 pt-6" />
        <Link to="/blog" className="mt-8 inline-block font-semibold text-turquoise underline">‹ {t('blog.retour')}</Link>
      </article>

      {aLireAussi.length > 0 && (
        <section className="bg-perle">
          <div className="mx-auto max-w-6xl px-4 py-10">
            <h2 className="text-2xl font-bold text-nuit">{t('blog.aLireAussi')}</h2>
            <div className="mt-6 grid grid-cols-1 gap-6 md:grid-cols-3">
              {aLireAussi.map((a) => <CarteArticle key={a.id} article={a} niveau="h3" />)}
            </div>
          </div>
        </section>
      )}
    </>
  )
}
