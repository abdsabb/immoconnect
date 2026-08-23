import { Link } from 'react-router'
import { useTranslation } from 'react-i18next'

// Page d'accueil — héro bleu nuit + appel à l'action corail (maquette Figure 13).
// Le moteur de recherche à trois critères et les biens récents arrivent au Sprint 2.
export default function Accueil() {
  const { t } = useTranslation()
  return (
    <section className="bg-nuit text-white">
      <div className="mx-auto max-w-6xl px-4 py-20">
        <h1 className="text-4xl md:text-5xl font-extrabold max-w-2xl">{t('accueil.titre')}</h1>
        <p className="mt-4 text-lg text-white/80 max-w-xl">{t('accueil.sousTitre')}</p>
        <Link to="/biens" className="mt-8 inline-block bg-corail hover:bg-corail/90 text-white font-titre font-bold px-6 py-3 rounded-lg">
          {t('accueil.rechercher')}
        </Link>
      </div>
    </section>
  )
}
