import { Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useFavoris } from '../services/favoris'
import CarteBien from '../components/CarteBien'

// Cas M2 : consulter sa liste de favoris (mêmes cartes que la recherche — gabarit « rubrique »)
export default function MesFavoris() {
  const { t } = useTranslation()
  const { estMembre, biens, isPending, isError } = useFavoris()

  return (
    <section className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-3xl font-bold text-nuit">{t('favori.titre')}</h1>

      {!estMembre && <p className="mt-6 text-gray-600">{t('favori.reserveAuxMembres')}</p>}
      {isPending && <p className="mt-6 text-gray-500">{t('commun.chargement')}</p>}
      {isError && <p role="alert" className="mt-6 text-erreur">{t('commun.erreurReseau')}</p>}

      {estMembre && !isPending && !isError && (
        biens.length === 0 ? (
          <div className="mt-6 space-y-3">
            <p className="text-gray-600">{t('favori.vide')}</p>
            <Link to="/biens" className="inline-block text-turquoise font-semibold underline">{t('accueil.rechercher')}</Link>
          </div>
        ) : (
          <>
            <p className="mt-6 text-sm text-gray-600">{t('favori.nombre', { count: biens.length })}</p>
            <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {biens.map((bien) => <CarteBien key={bien.id} bien={bien} />)}
            </div>
          </>
        )
      )}
    </section>
  )
}
