import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useTitrePage } from '../services/titre'
import { chargerArticles, chargerCategoriesBlog } from '../services/admin'
import CarteArticle from '../components/CarteArticle'

// Cas V5 « Lire les articles du blog » : articles publiés, classés par date, filtrables par catégorie.
// Chaque page compte dix articles : le premier en grand, les neuf suivants en grille de trois colonnes.
export default function Blog() {
  const { t } = useTranslation()
  useTitrePage(t('blog.titre'))
  const [categorieId, setCategorieId] = useState('')
  const [page, setPage] = useState(0)

  const categories = useQuery({ queryKey: ['blog-categories'], queryFn: chargerCategoriesBlog, staleTime: Infinity })
  const { data, isPending, isError } = useQuery({
    queryKey: ['articles', categorieId, page],
    queryFn: () => chargerArticles({ categorieId, page, taille: 10 }),
    placeholderData: keepPreviousData,
  })

  const choisir = (id) => {
    setCategorieId(id)
    setPage(0)
  }
  const changerDePage = (numero) => {
    setPage(numero)
    window.scrollTo({ top: 0 })
  }
  const filtre = (actif) =>
    `rounded-full px-4 py-1.5 text-sm font-semibold border transition-colors ${actif ? 'bg-nuit text-white border-nuit' : 'bg-white border-gray-300 text-nuit hover:border-turquoise hover:text-turquoise'}`
  const [premier, ...suivants] = data?.contenu ?? []

  return (
    <>
      <header className="bg-perle">
        <div className="mx-auto max-w-6xl px-4 py-10">
          <h1 className="text-4xl font-bold text-nuit">{t('blog.titre')}</h1>
          <p className="mt-2 max-w-2xl text-lg text-gray-600">{t('blog.sousTitre')}</p>
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
        </div>
      </header>

      <section className="mx-auto max-w-6xl px-4 py-10">
        {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
        {isError && <p role="alert" className="text-erreur">{t('commun.erreurReseau')}</p>}
        {data?.contenu.length === 0 && <p className="text-gray-600">{t('blog.vide')}</p>}

        {premier && (
          <CarteArticle article={premier} aLaUne etiquette={page === 0 && categorieId === '' ? t('blog.aLaUne') : undefined} />
        )}
        {suivants.length > 0 && (
          <div className="mt-8 grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
            {suivants.map((article) => <CarteArticle key={article.id} article={article} />)}
          </div>
        )}

        {data && data.totalPages > 1 && (
          <nav className="mt-10 flex items-center justify-center gap-3" aria-label="Pagination">
            <button type="button" disabled={page === 0} onClick={() => changerDePage(page - 1)}
              className="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-40">‹</button>
            <span className="text-sm text-gray-600">{t('commun.page', { page: page + 1, total: data.totalPages })}</span>
            <button type="button" disabled={page >= data.totalPages - 1} onClick={() => changerDePage(page + 1)}
              className="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-40">›</button>
          </nav>
        )}
      </section>
    </>
  )
}
