import { Suspense, lazy, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { cheminListe, rechercherBiens, TYPES_OFFRE } from '../services/biens'
import { chargerCategories } from '../services/annonces'
import CarteBien from '../components/CarteBien'
// La carte (Leaflet) se charge à part : la liste s'affiche sans l'attendre
const CarteResultats = lazy(() => import('../components/CarteResultats'))
import LienFlux from '../components/LienFlux'

const CRITERES = ['categorieId', 'ville', 'prixMin', 'prixMax', 'chambresMin', 'superficieMin', 'statut']
const TRI_PAR_DEFAUT = 'publieLe,desc'
const TRIS = { [TRI_PAR_DEFAUT]: 'triRecent', 'prix,asc': 'triPrixAsc', 'prix,desc': 'triPrixDesc', 'superficie,desc': 'triSurface' }
const REASSURANCES = ['visites', 'gratuit', 'paiement', 'adresse']
// Un bien hors ligne n'est jamais public ; « vendu » ne concerne qu'une vente, « loué » qu'une location
const statutsPour = (typeOffre) => ['sous_option', 'vendu', 'loue'].filter((s) => s !== { vente: 'loue', location: 'vendu' }[typeOffre])

function Champ({ libelle, children }) {
  return (
    <label className="block text-sm text-gray-600">
      {libelle}
      <span className="mt-1 block">{children}</span>
    </label>
  )
}

// Liste des biens (gabarit « rubrique », maquette Figure 14) : filtres à gauche avec application
// explicite, résultats au centre, carte OpenStreetMap à droite, tri en haut à droite.
// Trois adresses pour une même page : /a-vendre et /a-louer fixent le type d'offre, /biens montre tout.
// Les critères vivent dans l'adresse de la page : la recherche de l'accueil arrive ici toute faite,
// un résultat se partage par son lien et le bouton « précédent » du navigateur revient à la recherche d'avant.
export default function Biens({ typeOffre: typeImpose }) {
  const { t } = useTranslation()
  const [adresse, setAdresse] = useSearchParams()
  const [filtresOuverts, setFiltresOuverts] = useState(false)
  const [survol, setSurvol] = useState(null)

  const typeChoisi = TYPES_OFFRE.includes(adresse.get('typeOffre')) ? adresse.get('typeOffre') : ''
  const typeOffre = typeImpose ?? typeChoisi
  const criteres = Object.fromEntries(CRITERES.map((nom) => [nom, adresse.get(nom) ?? '']))
  const tri = adresse.get('tri') in TRIS ? adresse.get('tri') : TRI_PAR_DEFAUT
  const page = Math.max(0, Number.parseInt(adresse.get('page') ?? '0', 10) || 0)

  const categories = useQuery({ queryKey: ['categories'], queryFn: chargerCategories, staleTime: Infinity })
  const { data, isPending, isError } = useQuery({
    queryKey: ['biens', typeOffre, criteres, tri, page],
    queryFn: () => rechercherBiens({ ...criteres, typeOffre, tri, page, taille: 12 }),
    placeholderData: keepPreviousData,
  })

  // Le tri et la page s'ajoutent aux critères en cours ; des critères nouveaux ramènent à la première page
  const avecTri = (parametres, choix) => {
    if (choix !== TRI_PAR_DEFAUT) parametres.set('tri', choix)
    else parametres.delete('tri')
    return parametres
  }
  const appliquer = (e) => {
    e.preventDefault()
    const saisie = [...new FormData(e.currentTarget)].filter(([, valeur]) => valeur !== '')
    setAdresse(avecTri(new URLSearchParams(saisie), tri))
    setFiltresOuverts(false)
  }
  const trier = (choix) => {
    const suite = new URLSearchParams(adresse)
    suite.delete('page')
    setAdresse(avecTri(suite, choix))
  }
  const allerPage = (numero) => {
    const suite = new URLSearchParams(adresse)
    if (numero > 0) suite.set('page', numero)
    else suite.delete('page')
    setAdresse(suite)
    window.scrollTo({ top: 0 })
  }

  const location = typeOffre === 'location'
  const champ = 'w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-nuit'
  const titre = t(typeImpose ? `biens.titre_${typeImpose}` : 'biens.titre')
  const filtresActifs = CRITERES.some((nom) => criteres[nom] !== '') || (!typeImpose && typeChoisi !== '')

  return (
    <section className="bg-perle">
      <div className="mx-auto max-w-6xl px-4 py-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <nav aria-label="Fil d'Ariane" className="text-sm text-gray-500">
            <Link to="/" className="hover:text-turquoise">{t('nav.accueil')}</Link> › <span className="text-nuit">{titre}</span>
          </nav>
          {/* Le flux suit le type d'offre de la page, pas les filtres : il annonce les nouveautés */}
          <LienFlux chemin={typeOffre ? `biens?typeOffre=${typeOffre}` : 'biens'} titre={t('biens.flux', { titre })} />
        </div>

        <div className="mt-2 flex flex-wrap items-end justify-between gap-3">
          <h1 className="text-3xl font-bold text-nuit">
            {titre}{' '}
            {data && <span className="text-base font-normal text-gray-600">({t('biens.nombre', { count: data.totalElements })})</span>}
          </h1>
          <label className="flex items-center gap-2 text-sm text-gray-700">
            {t('biens.tri')}
            <select value={tri} onChange={(e) => trier(e.target.value)} className="rounded-lg border border-gray-300 bg-white px-3 py-2 font-semibold text-nuit">
              {Object.entries(TRIS).map(([valeur, cle]) => <option key={valeur} value={valeur}>{t(`biens.${cle}`)}</option>)}
            </select>
          </label>
        </div>

        <div className="mt-6 grid grid-cols-1 items-start gap-6 lg:grid-cols-[15rem_minmax(0,1fr)] xl:grid-cols-[15rem_minmax(0,1fr)_18rem]">
          <aside className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm lg:sticky lg:top-4">
            <button type="button" onClick={() => setFiltresOuverts(!filtresOuverts)} aria-expanded={filtresOuverts} aria-controls="filtres"
              className="flex w-full items-center justify-between font-titre text-lg font-bold text-nuit lg:hidden">
              {t('biens.filtresTitre')}{filtresActifs && ' •'}
              <span aria-hidden="true">{filtresOuverts ? '−' : '+'}</span>
            </button>
            <h2 className="hidden font-titre text-lg font-bold text-nuit lg:block">{t('biens.filtresTitre')}</h2>

            {/* La clé recrée le formulaire quand l'adresse change : ses champs repartent des critères en cours */}
            <form id="filtres" key={`${typeImpose}?${adresse}`} onSubmit={appliquer} aria-label={t('biens.filtres')}
              className={`mt-4 space-y-4 ${filtresOuverts ? '' : 'hidden'} lg:block`}>
              {!typeImpose && (
                <Champ libelle={t('annonce.typeOffre')}>
                  <select name="typeOffre" defaultValue={typeChoisi} className={champ}>
                    <option value="">{t('biens.toutesOffres')}</option>
                    {TYPES_OFFRE.map((type) => <option key={type} value={type}>{t(`offre.${type}`)}</option>)}
                  </select>
                </Champ>
              )}
              <Champ libelle={t('biens.categorie')}>
                <select name="categorieId" defaultValue={criteres.categorieId} className={champ}>
                  <option value="">{t('biens.toutesCategories')}</option>
                  {/* Tant que la liste charge, la catégorie demandée par l'adresse reste sélectionnée */}
                  {!categories.data && criteres.categorieId && <option value={criteres.categorieId}>…</option>}
                  {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.nom}</option>)}
                </select>
              </Champ>
              <Champ libelle={t('biens.villeOuCode')}>
                <input name="ville" defaultValue={criteres.ville} placeholder="1050, Ixelles…" className={champ} />
              </Champ>
              <fieldset>
                <legend className="text-sm text-gray-600">{t(location ? 'biens.loyer' : 'biens.budget')}</legend>
                <div className="mt-1 flex items-center gap-2">
                  <input name="prixMin" defaultValue={criteres.prixMin} type="number" min="0" step={location ? 50 : 1000}
                    placeholder={t('biens.min')} aria-label={t('biens.prixMin')} className={champ} />
                  <span aria-hidden="true">–</span>
                  <input name="prixMax" defaultValue={criteres.prixMax} type="number" min="0" step={location ? 50 : 1000}
                    placeholder={t('biens.max')} aria-label={t(location ? 'biens.loyerMax' : 'biens.prixMax')} className={champ} />
                </div>
              </fieldset>
              <Champ libelle={t('biens.chambresMin')}>
                <input name="chambresMin" defaultValue={criteres.chambresMin} type="number" min="0" max="20" className={champ} />
              </Champ>
              <Champ libelle={t('biens.superficieMin')}>
                <input name="superficieMin" defaultValue={criteres.superficieMin} type="number" min="0" step="5" className={champ} />
              </Champ>
              <Champ libelle={t('bien.statutLabel')}>
                <select name="statut" defaultValue={criteres.statut} className={champ}>
                  <option value="">{t('statut.disponible')}</option>
                  {statutsPour(typeOffre).map((s) => <option key={s} value={s}>{t(`statut.${s}`)}</option>)}
                </select>
              </Champ>
              <button type="submit" className="w-full rounded-full bg-turquoise px-4 py-2.5 font-titre font-bold text-white hover:bg-turquoise/90">
                {t('biens.appliquer')}
              </button>
              {filtresActifs && (
                <Link to={cheminListe(typeImpose)} onClick={() => setFiltresOuverts(false)} className="block text-center text-sm text-turquoise underline">
                  {t('biens.reinitialiser')}
                </Link>
              )}
            </form>
          </aside>

          <div>
            {isError && <p role="alert" className="text-erreur">{t('biens.erreur')}</p>}
            {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
            {data?.contenu.length === 0 && <p className="rounded-xl bg-white p-6 text-gray-600">{t('biens.vide')}</p>}
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              {data?.contenu.map((bien) => (
                <div key={bien.id} onMouseEnter={() => setSurvol(bien.id)} onMouseLeave={() => setSurvol(null)}
                  className={`grid rounded-xl transition-shadow ${survol === bien.id ? 'ring-2 ring-corail' : ''}`}>
                  <CarteBien bien={bien} />
                </div>
              ))}
            </div>
            {data && data.totalPages > 1 && (
              <nav className="mt-6 flex flex-wrap items-center justify-center gap-2 rounded-full border border-gray-200 bg-white px-4 py-2" aria-label="Pagination">
                <button type="button" disabled={page === 0} onClick={() => allerPage(page - 1)} aria-label={t('biens.pagePrecedente')}
                  className="px-3 py-1.5 rounded-full disabled:opacity-40 hover:bg-perle">‹</button>
                {Array.from({ length: data.totalPages }, (_, i) => (
                  <button key={i} type="button" onClick={() => allerPage(i)} aria-current={i === page ? 'page' : undefined}
                    className={`px-3 py-1.5 rounded-full ${i === page ? 'bg-turquoise text-white font-bold' : 'hover:bg-perle'}`}>
                    {i + 1}
                  </button>
                ))}
                <button type="button" disabled={page >= data.totalPages - 1} onClick={() => allerPage(page + 1)} aria-label={t('biens.pageSuivante')}
                  className="px-3 py-1.5 rounded-full disabled:opacity-40 hover:bg-perle">›</button>
              </nav>
            )}
          </div>

          <aside className="lg:col-span-2 xl:col-span-1 xl:sticky xl:top-4" aria-label={t('biens.carte')}>
            <div className="rounded-xl border border-gray-200 bg-white p-3 shadow-sm">
              <h2 className="px-1 pb-2 font-titre font-bold text-nuit">{t('biens.carte')}</h2>
              <Suspense fallback={<div className="h-80 xl:h-[32rem] rounded-xl bg-perle" aria-hidden="true" />}>
                <CarteResultats biens={data?.contenu ?? []} actif={survol} surSurvol={setSurvol} className="h-80 xl:h-[32rem]" />
              </Suspense>
            </div>
          </aside>
        </div>

        <ul className="mt-8 flex flex-wrap gap-x-8 gap-y-2 text-sm text-gray-600">
          {REASSURANCES.map((cle) => <li key={cle}>{t(`biens.reassurance.${cle}`)}</li>)}
        </ul>
      </div>
    </section>
  )
}
