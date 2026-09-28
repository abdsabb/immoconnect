import { useSearchParams } from 'react-router'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { rechercherBiens } from '../services/biens'
import { chargerCategories } from '../services/annonces'
import CarteBien from '../components/CarteBien'

const CRITERES = ['ville', 'categorieId', 'prixMax', 'chambresMin']
const TRI_PAR_DEFAUT = 'publieLe,desc'
const TRIS = [TRI_PAR_DEFAUT, 'prix,asc', 'prix,desc', 'superficie,desc']

// Liste des biens (gabarit « rubrique ») : filtres du cas V2, résultats en cartes, pagination.
// Les critères vivent dans l'adresse de la page : la recherche de l'accueil arrive ici toute faite,
// un résultat se partage par son lien et le bouton « précédent » du navigateur revient à la recherche d'avant.
export default function Biens() {
  const { t } = useTranslation()
  const [adresse, setAdresse] = useSearchParams()

  const criteres = Object.fromEntries(CRITERES.map((nom) => [nom, adresse.get(nom) ?? '']))
  const tri = TRIS.includes(adresse.get('tri')) ? adresse.get('tri') : TRI_PAR_DEFAUT
  const page = Math.max(0, Number.parseInt(adresse.get('page') ?? '0', 10) || 0)

  const categories = useQuery({ queryKey: ['categories'], queryFn: chargerCategories, staleTime: Infinity })
  const { data, isPending, isError } = useQuery({
    queryKey: ['biens', criteres, tri, page],
    queryFn: () => rechercherBiens({ ...criteres, tri, page, taille: 12 }),
    placeholderData: keepPreviousData,
  })

  const soumettre = (e) => {
    e.preventDefault()
    const saisie = [...new FormData(e.currentTarget)].filter(([nom, valeur]) => valeur !== '' && !(nom === 'tri' && valeur === TRI_PAR_DEFAUT))
    setAdresse(new URLSearchParams(saisie))
  }
  const allerPage = (numero) => {
    const suite = new URLSearchParams(adresse)
    if (numero > 0) suite.set('page', numero)
    else suite.delete('page')
    setAdresse(suite)
    window.scrollTo({ top: 0 })
  }
  const champ = 'rounded-lg border border-gray-300 px-3 py-2'

  return (
    <section className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-3xl font-bold text-nuit">{t('biens.titre')}</h1>

      {/* La clé recrée le formulaire quand l'adresse change : ses champs repartent des critères en cours */}
      <form key={adresse.toString()} onSubmit={soumettre} aria-label={t('biens.filtres')}
        className="mt-6 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 bg-perle p-4 rounded-xl">
        <input name="ville" defaultValue={criteres.ville} placeholder={t('biens.ville')} aria-label={t('biens.ville')} className={champ} />
        <select name="categorieId" defaultValue={criteres.categorieId} aria-label={t('biens.categorie')} className={`${champ} bg-white`}>
          <option value="">{t('biens.toutesCategories')}</option>
          {/* Tant que la liste charge, la catégorie demandée par l'adresse reste sélectionnée */}
          {!categories.data && criteres.categorieId && <option value={criteres.categorieId}>…</option>}
          {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.nom}</option>)}
        </select>
        <input name="prixMax" defaultValue={criteres.prixMax} type="number" min="0" step="1000" placeholder={t('biens.prixMax')} aria-label={t('biens.prixMax')} className={champ} />
        <input name="chambresMin" defaultValue={criteres.chambresMin} type="number" min="0" max="20" placeholder={t('biens.chambresMin')} aria-label={t('biens.chambresMin')} className={champ} />
        <select name="tri" defaultValue={tri} aria-label={t('biens.tri')} className={`${champ} bg-white`}>
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
            <nav className="mt-8 flex flex-wrap items-center justify-center gap-2" aria-label="Pagination">
              <button type="button" disabled={page === 0} onClick={() => allerPage(page - 1)}
                className="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-40">‹</button>
              {Array.from({ length: data.totalPages }, (_, i) => (
                <button key={i} type="button" onClick={() => allerPage(i)} aria-current={i === page ? 'page' : undefined}
                  className={`px-3 py-2 rounded-lg border ${i === page ? 'bg-turquoise text-white border-turquoise' : 'border-gray-300 hover:bg-perle'}`}>
                  {i + 1}
                </button>
              ))}
              <button type="button" disabled={page >= data.totalPages - 1} onClick={() => allerPage(page + 1)}
                className="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-40">›</button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}
