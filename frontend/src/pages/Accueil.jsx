import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { cheminListe, rechercherBiens, TYPES_OFFRE } from '../services/biens'
import { chargerCategories } from '../services/annonces'
import { chargerConfigPaiement, formatMontant } from '../services/rendezVous'
import { chargerArticles, formatDate } from '../services/admin'
import { useAuth } from '../auth/AuthContext'
import CarteBien from '../components/CarteBien'
import photoBruxelles from '../assets/accueil-bruxelles.jpg'

const ETAPES = ['choisir', 'reserver', 'visiter']

function Section({ titre, sousTitre, lien, fond = '', children }) {
  return (
    <section className={fond}>
      <div className="mx-auto max-w-6xl px-4 py-14">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-2xl md:text-3xl font-bold text-nuit">{titre}</h2>
            {sousTitre && <p className="mt-1 text-gray-600">{sousTitre}</p>}
          </div>
          {lien}
        </div>
        {children}
      </div>
    </section>
  )
}

// Page d'accueil (gabarit « accueil », livrable 10, § 8.1) : moteur de recherche dans le héro (acheter ou
// louer, puis trois critères), biens récents, bandeau de la prise de rendez-vous en ligne, blog en bas de page.
// Le titre et le sous-titre sont des textes du site : l'administrateur les modifie depuis le back-office (cas A6).
export default function Accueil() {
  const { t, i18n } = useTranslation()
  const naviguer = useNavigate()
  const { estConnecte } = useAuth()
  const langue = i18n.resolvedLanguage

  const [offreCherchee, setOffreCherchee] = useState('vente')
  const ventes = useQuery({ queryKey: ['biens', 'accueil', 'vente'], queryFn: () => rechercherBiens({ typeOffre: 'vente', taille: 3 }) })
  const locations = useQuery({ queryKey: ['biens', 'accueil', 'location'], queryFn: () => rechercherBiens({ typeOffre: 'location', taille: 3 }) })
  const recents = { vente: ventes, location: locations }
  const categories = useQuery({ queryKey: ['categories'], queryFn: chargerCategories, staleTime: Infinity })
  const paiement = useQuery({ queryKey: ['paiement-config'], queryFn: chargerConfigPaiement, staleTime: Infinity })
  const articles = useQuery({ queryKey: ['articles', 'accueil'], queryFn: () => chargerArticles({ taille: 3 }) })

  const rechercher = (e) => {
    e.preventDefault()
    const criteres = [...new FormData(e.currentTarget)].filter(([nom, valeur]) => valeur !== '' && nom !== 'typeOffre')
    naviguer(`${cheminListe(offreCherchee)}?${new URLSearchParams(criteres)}`)
  }
  const lienSection = 'text-turquoise font-semibold underline underline-offset-4 hover:text-nuit'
  const champ = 'w-full rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-nuit'
  const prixPremium = paiement.data ? formatMontant(paiement.data.prixCreneauPremium, langue) : '15 €'

  return (
    <>
      <section className="relative bg-nuit text-white">
        <img src={photoBruxelles} alt="" className="absolute inset-0 h-full w-full object-cover opacity-25" />
        <div className="relative mx-auto max-w-6xl px-4 py-16 md:py-24">
          <h1 className="text-4xl md:text-5xl font-extrabold max-w-2xl">{t('contenu:accueil.titre', { defaultValue: t('accueil.titre') })}</h1>
          <p className="mt-4 text-lg text-white/85 max-w-xl">{t('contenu:accueil.sous_titre', { defaultValue: t('accueil.sousTitre') })}</p>

          <form onSubmit={rechercher} role="search" aria-label={t('accueil.rechercher')}
            className="mt-8 grid grid-cols-1 md:grid-cols-4 gap-3 rounded-xl bg-white p-4 shadow-lg">
            <fieldset className="md:col-span-4 flex gap-2">
              <legend className="sr-only">{t('annonce.typeOffre')}</legend>
              {TYPES_OFFRE.map((type) => (
                <label key={type} className={`cursor-pointer rounded-full px-4 py-1.5 text-sm font-semibold border ${
                  offreCherchee === type ? 'bg-nuit text-white border-nuit' : 'border-gray-300 text-nuit hover:bg-perle'}`}>
                  <input type="radio" name="typeOffre" value={type} checked={offreCherchee === type}
                    onChange={() => setOffreCherchee(type)} className="sr-only" />
                  {t(`accueil.recherche.${type}`)}
                </label>
              ))}
            </fieldset>
            <label className="text-sm font-semibold text-nuit">
              {t('biens.ville')}
              <input name="ville" placeholder={t('accueil.recherche.villeExemple')} className={`mt-1 font-normal ${champ}`} />
            </label>
            <label className="text-sm font-semibold text-nuit">
              {t('accueil.recherche.categorie')}
              <select name="categorieId" defaultValue="" className={`mt-1 font-normal ${champ}`}>
                <option value="">{t('accueil.recherche.toutes')}</option>
                {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.nom}</option>)}
              </select>
            </label>
            <label className="text-sm font-semibold text-nuit">
              {t(offreCherchee === 'location' ? 'biens.loyerMax' : 'biens.prixMax')}
              <input name="prixMax" type="number" min="0" step={offreCherchee === 'location' ? 50 : 10000}
                placeholder={offreCherchee === 'location' ? '1200' : '350000'} className={`mt-1 font-normal ${champ}`} />
            </label>
            <button type="submit" className="self-end rounded-lg bg-corail px-4 py-2.5 font-titre font-bold text-white hover:bg-corail/90">
              {t('accueil.rechercher')}
            </button>
          </form>
        </div>
      </section>

      <section className="bg-perle">
        <dl className="mx-auto grid max-w-6xl grid-cols-2 gap-6 px-4 py-8 md:grid-cols-4">
          {[
            [ventes.data?.totalElements ?? '—', t('accueil.chiffres.vente')],
            [locations.data?.totalElements ?? '—', t('accueil.chiffres.location')],
            [categories.data?.length ?? '—', t('accueil.chiffres.categories')],
            ['7 / 7', t('accueil.chiffres.visites')],
          ].map(([valeur, libelle]) => (
            <div key={libelle} className="flex flex-col-reverse text-center">
              <dt className="text-sm text-gray-600">{libelle}</dt>
              <dd className="font-titre text-2xl font-extrabold text-nuit md:text-3xl">{valeur}</dd>
            </div>
          ))}
        </dl>
      </section>

      {TYPES_OFFRE.map((type, i) => (
        <Section key={type} titre={t(`accueil.recents.${type}`)} fond={i % 2 ? 'bg-perle' : ''}
          lien={<Link to={cheminListe(type)} className={lienSection}>{t(`accueil.recents.tout_${type}`)}</Link>}>
          {recents[type].isPending && <p className="mt-6 text-gray-500">{t('commun.chargement')}</p>}
          {recents[type].isError && <p role="alert" className="mt-6 text-erreur">{t('biens.erreur')}</p>}
          <div className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {recents[type].data?.contenu.map((bien) => <CarteBien key={bien.id} bien={bien} niveau="h3" />)}
          </div>
        </Section>
      ))}

      {categories.data?.length > 0 && (
        <Section titre={t('accueil.categories.titre')}>
          <ul className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-5">
            {categories.data.map((c) => (
              <li key={c.id}>
                <Link to={`/biens?categorieId=${c.id}`}
                  className="flex h-full flex-col rounded-xl border border-gray-200 bg-white p-4 shadow-sm transition-shadow hover:border-turquoise hover:shadow-md">
                  <span className="font-titre font-bold text-nuit">{c.nom}</span>
                  <span className="mt-1 text-sm text-gray-600">{c.description}</span>
                </Link>
              </li>
            ))}
          </ul>
        </Section>
      )}

      <section className="bg-nuit text-white">
        <div className="mx-auto max-w-6xl px-4 py-14">
          <h2 className="text-2xl md:text-3xl font-bold">{t('accueil.visite.titre')}</h2>
          <p className="mt-2 max-w-2xl text-white/85">{t('accueil.visite.intro')}</p>
          <ol className="mt-8 grid grid-cols-1 gap-6 md:grid-cols-3">
            {ETAPES.map((etape, i) => (
              <li key={etape} className="rounded-xl bg-white/10 p-5">
                <span className="flex h-9 w-9 items-center justify-center rounded-full bg-corail font-titre font-bold" aria-hidden="true">{i + 1}</span>
                <h3 className="mt-3 font-bold">{t(`accueil.visite.${etape}.titre`)}</h3>
                <p className="mt-1 text-sm text-white/85">{t(`accueil.visite.${etape}.texte`)}</p>
              </li>
            ))}
          </ol>
          <div className="mt-8 grid grid-cols-1 gap-4 md:grid-cols-2">
            <p className="rounded-xl border border-white/20 p-5">
              <strong className="block font-titre">{t('accueil.visite.standard.titre')}</strong>
              <span className="text-sm text-white/85">{t('accueil.visite.standard.texte')}</span>
            </p>
            <p className="rounded-xl border border-ambre/60 p-5">
              <strong className="block font-titre text-ambre">{t('accueil.visite.premium.titre', { prix: prixPremium })}</strong>
              <span className="text-sm text-white/85">{t('accueil.visite.premium.texte')}</span>
            </p>
          </div>
          <Link to="/biens" className="mt-8 inline-block rounded-lg bg-corail px-6 py-3 font-titre font-bold text-white hover:bg-corail/90">
            {t('accueil.visite.bouton')}
          </Link>
        </div>
      </section>

      {articles.data?.contenu.length > 0 && (
        <Section titre={t('accueil.blog.titre')} sousTitre={t('blog.sousTitre')}
          lien={<Link to="/blog" className={lienSection}>{t('accueil.blog.voirTout')}</Link>}>
          <div className="mt-6 grid grid-cols-1 gap-6 md:grid-cols-3">
            {articles.data.contenu.map((article) => (
              <article key={article.id} className="flex flex-col gap-2 rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
                <p className="text-xs font-semibold uppercase tracking-wide text-turquoise">{article.categorie}</p>
                <h3 className="font-titre font-bold leading-snug text-nuit">
                  <Link to={`/blog/${article.id}`} className="hover:text-turquoise">{article.titre}</Link>
                </h3>
                <p className="text-sm text-gray-600">{article.extrait}</p>
                <p className="mt-auto text-xs text-gray-500">{formatDate(article.publieLe, langue)} · {article.auteur}</p>
              </article>
            ))}
          </div>
        </Section>
      )}

      {!estConnecte && (
        <section className="bg-perle">
          <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-10">
            <div>
              <h2 className="text-2xl font-bold text-nuit">{t('accueil.compte.titre')}</h2>
              <p className="mt-1 text-gray-600">{t('accueil.compte.texte')}</p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Link to="/inscription" className="rounded-lg bg-corail px-6 py-3 font-titre font-bold text-white hover:bg-corail/90">{t('nav.inscription')}</Link>
              <Link to="/connexion" className="rounded-lg border-2 border-nuit px-6 py-3 font-titre font-bold text-nuit hover:bg-white">{t('nav.connexion')}</Link>
            </div>
          </div>
        </section>
      )}
    </>
  )
}
