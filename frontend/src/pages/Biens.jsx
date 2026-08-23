import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { rechercherBiens } from '../services/biens'
import CarteBien from '../components/CarteBien'

const FILTRES_INITIAUX = { ville: '', prixMax: '', chambresMin: '', tri: 'publieLe,desc' }

// Liste des biens (gabarit « rubrique ») : filtres du cas V2, résultats en cartes, pagination.
export default function Biens() {
  const { t } = useTranslation()
  const [formulaire, setFormulaire] = useState(FILTRES_INITIAUX)
  const [criteres, setCriteres] = useState(FILTRES_INITIAUX)
  const [page, setPage] = useState(0)

  const { data, isPending, isError } = useQuery({
    queryKey: ['biens', criteres, page],
    queryFn: () => rechercherBiens({ ...criteres, page, taille: 12 }),
    placeholderData: keepPreviousData,
  })

  const soumettre = (e) => {
    e.preventDefault()
    setPage(0)
    setCriteres(formulaire)
  }
  const champ = (nom) => ({ value: formulaire[nom], onChange: (e) => setFormulaire({ ...formulaire, [nom]: e.target.value }) })

  return (
    <section className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-3xl font-bold text-nuit">{t('biens.titre')}</h1>

      <form onSubmit={soumettre} className="mt-6 grid grid-cols-1 md:grid-cols-5 gap-3 bg-perle p-4 rounded-xl" aria-label={t('biens.filtres')}>
        <input {...champ('ville')} placeholder={t('biens.ville')} className="rounded-lg border border-gray-300 px-3 py-2" />
        <input {...champ('prixMax')} type="number" min="0" step="1000" placeholder={t('biens.prixMax')} className="rounded-lg border border-gray-300 px-3 py-2" />
        <input {...champ('chambresMin')} type="number" min="0" max="20" placeholder={t('biens.chambresMin')} className="rounded-lg border border-gray-300 px-3 py-2" />
        <select {...champ('tri')} className="rounded-lg border border-gray-300 px-3 py-2 bg-white">
          <option value="publieLe,desc">{t('biens.triRecent')}</option>
          <option value="prix,asc">{t('biens.triPrixAsc')}</option>
          <option value="prix,desc">{t('biens.triPrixDesc')}</option>
          <option value="superficie,desc">{t('biens.triSurface')}</option>
        </select>
        <button type="submit" className="bg-corail hover:bg-corail/90 text-white font-titre font-bold rounded-lg px-4 py-2">
          {t('accueil.rechercher')}
        </button>
      </form>

      {isError && <p role="alert" className="mt-6 text-erreur">{t('biens.erreur')}</p>}
      {isPending && <p className="mt-6 text-gray-500">{t('commun.chargement')}</p>}

      {data && (
        <>
          <p className="mt-6 text-sm text-gray-600">{t('biens.resultats', { count: data.totalElements })}</p>
          {data.contenu.length === 0 ? (
            <p className="mt-4 text-gray-600">{t('biens.vide')}</p>
          ) : (
            <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {data.contenu.map((bien) => <CarteBien key={bien.id} bien={bien} />)}
            </div>
          )}
          {data.totalPages > 1 && (
            <nav className="mt-8 flex items-center justify-center gap-2" aria-label="Pagination">
              <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}
                className="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-40">‹</button>
              {Array.from({ length: data.totalPages }, (_, i) => (
                <button key={i} type="button" onClick={() => setPage(i)} aria-current={i === page ? 'page' : undefined}
                  className={`px-3 py-2 rounded-lg border ${i === page ? 'bg-turquoise text-white border-turquoise' : 'border-gray-300 hover:bg-perle'}`}>
                  {i + 1}
                </button>
              ))}
              <button type="button" disabled={page >= data.totalPages - 1} onClick={() => setPage(page + 1)}
                className="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-40">›</button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}
