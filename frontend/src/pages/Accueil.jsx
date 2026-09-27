import { Link } from 'react-router'
import { useTranslation } from 'react-i18next'

// Page d'accueil — héro bleu nuit + appel à l'action corail (maquette Figure 13).
// Le titre et le sous-titre sont des textes du site : l'administrateur les modifie depuis le back-office (cas A6).
export default function Accueil() {
  const { t } = useTranslation()
  return (
    <section className="bg-nuit text-white">
      <div className="mx-auto max-w-6xl px-4 py-20">
        <h1 className="text-4xl md:text-5xl font-extrabold max-w-2xl">{t('contenu:accueil.titre', { defaultValue: t('accueil.titre') })}</h1>
        <p className="mt-4 text-lg text-white/80 max-w-xl">{t('contenu:accueil.sous_titre', { defaultValue: t('accueil.sousTitre') })}</p>
        <Link to="/biens" className="mt-8 inline-block bg-corail hover:bg-corail/90 text-white font-titre font-bold px-6 py-3 rounded-lg">
          {t('accueil.rechercher')}
        </Link>
      </div>
    </section>
  )
}
