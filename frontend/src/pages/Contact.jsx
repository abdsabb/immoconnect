import { Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useConfiguration } from '../services/configuration'
import { useTitrePage } from '../services/titre'

// Page « Contact » (quatrième entrée de la navigation, charte graphique) : les coordonnées et les horaires
// de l'agence, réglés par l'administrateur dans les paramètres du site (cas A5).
export default function Contact() {
  const { t } = useTranslation()
  const { data, isPending } = useConfiguration()
  const site = data?.site
  useTitrePage(t('nav.contact'))

  return (
    <section className="mx-auto max-w-6xl px-4 py-10">
      <nav aria-label="Fil d'Ariane" className="text-sm text-gray-500">
        <Link to="/" className="hover:text-turquoise">{t('nav.accueil')}</Link> › <span className="text-nuit">{t('nav.contact')}</span>
      </nav>
      <h1 className="mt-3 text-3xl font-bold text-nuit">{t('contact.titre')}</h1>
      <p className="mt-1 text-gray-600">{site?.slogan ?? t('contact.sousTitre')}</p>
      {isPending && <p className="mt-6 text-gray-500">{t('commun.chargement')}</p>}

      {site && (
        <div className="mt-8 grid gap-6 md:grid-cols-3">
          <Carte titre={t('contact.adresse')}>
            <p className="font-semibold text-nuit">{site.nom}</p>
            <address className="not-italic text-gray-700">{site.adresse}</address>
          </Carte>
          <Carte titre={t('contact.joindre')}>
            <p><a href={`tel:${site.telephone.replace(/[^+\d]/g, '')}`} className="font-semibold text-turquoise hover:underline">{site.telephone}</a></p>
            <p><a href={`mailto:${site.email}`} className="font-semibold text-turquoise hover:underline break-all">{site.email}</a></p>
          </Carte>
          <Carte titre={t('contact.horaires')}>
            <p className="text-gray-700">{site.horaires}</p>
            <p className="mt-2 text-sm text-gray-600">{t('contact.visites')}</p>
          </Carte>
        </div>
      )}

      <div className="mt-8 rounded-xl bg-perle p-6">
        <h2 className="text-xl font-bold text-nuit">{t('contact.agentTitre')}</h2>
        <p className="mt-1 text-gray-700">{t('contact.agentTexte')}</p>
        <div className="mt-4 flex flex-wrap gap-3">
          <Link to="/a-vendre" className="rounded-lg bg-corail px-4 py-2 font-titre font-bold text-white hover:bg-corail/90">{t('offre.vente')}</Link>
          <Link to="/a-louer" className="rounded-lg border-2 border-nuit px-4 py-2 font-titre font-bold text-nuit hover:bg-white">{t('offre.location')}</Link>
        </div>
      </div>
    </section>
  )
}

function Carte({ titre, children }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
      <h2 className="text-xs font-semibold uppercase tracking-wide text-gray-500">{titre}</h2>
      <div className="mt-2 space-y-1">{children}</div>
    </div>
  )
}
