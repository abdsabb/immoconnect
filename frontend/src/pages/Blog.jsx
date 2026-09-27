import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerArticles, chargerCategoriesBlog, formatDate } from '../services/admin'

// Cas V5 « Lire les articles du blog » : articles publiés, classés par date, filtrables par catégorie
export default function Blog() {
  const { t, i18n } = useTranslation()
  const [categorieId, setCategorieId] = useState('')
  const [page, setPage] = useState(0)

  const categories = useQuery({ queryKey: ['blog-categories'], queryFn: chargerCategoriesBlog, staleTime: Infinity })
  const { data, isPending, isError } = useQuery({
    queryKey: ['articles', categorieId, page],
    queryFn: () => chargerArticles({ categorieId, page, taille: 9 }),
    placeholderData: keepPreviousData,
  })

  const choisir = (id) => {
    setCategorieId(id)
    setPage(0)
  }
  const filtre = (actif) =>
    `rounded-full px-3 py-1 text-sm font-semibold border ${actif ? 'bg-turquoise text-white border-turquoise' : 'border-gray-300 text-nuit hover:bg-perle'}`

  return (
    <section className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-3xl font-bold text-nuit">{t('blog.titre')}</h1>
      <p className="mt-1 text-gray-600">{t('blog.sousTitre')}</p>

      <div className="mt-6 flex flex-wrap gap-2" role="group" aria-label={t('blog.categories')}>
        <button type="button" onClick={() => choisir('')} aria-pressed={categorieId === ''} className={filtre(categorieId === '')}>
          {t('blog.toutes')}
        </button>
        {categories.data?.map((c) => (
          <button key={c.id} type="button" onClick={() => choisir(c.id)} aria-pressed={categorieId === c.id} className={filtre(categorieId === c.id)}>
            {c.nom}
          </button>
        ))}
      </div>

      {isPending && <p className="mt-6 text-gray-500">{t('commun.chargement')}</p>}
      {isError && <p role="alert" className="mt-6 text-erreur">{t('commun.erreurReseau')}</p>}
      {data?.contenu.length === 0 && <p className="mt-6 text-gray-600">{t('blog.vide')}</p>}

      <div className="mt-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {data?.contenu.map((article) => (
          <article key={article.id} className="bg-white border border-gray-200 rounded-xl shadow-sm p-5 flex flex-col gap-2">
            <p className="text-xs uppercase tracking-wide text-turquoise font-semibold">{article.categorie}</p>
            <h2 className="font-titre font-bold text-nuit leading-snug">
              <Link to={`/blog/${article.id}`} className="hover:text-turquoise">{article.titre}</Link>
            </h2>
            <p className="text-sm text-gray-600">{article.extrait}</p>
            <p className="mt-auto text-xs text-gray-500">{formatDate(article.publieLe, i18n.resolvedLanguage)} · {article.auteur}</p>
          </article>
        ))}
      </div>

      {data && data.totalPages > 1 && (
        <nav className="mt-8 flex items-center justify-center gap-3" aria-label="Pagination">
          <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}
            className="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-40">‹</button>
          <span className="text-sm text-gray-600">{t('commun.page', { page: page + 1, total: data.totalPages })}</span>
          <button type="button" disabled={page >= data.totalPages - 1} onClick={() => setPage(page + 1)}
            className="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-40">›</button>
        </nav>
      )}
    </section>
  )
}
