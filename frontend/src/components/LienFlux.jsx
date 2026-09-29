import { useTranslation } from 'react-i18next'

// Lien vers un flux RSS public. Lien ordinaire, ouvert à part : un flux est un document XML servi
// par l'API, pas une page de l'application.
export default function LienFlux({ chemin, titre, className = '' }) {
  const { t } = useTranslation()
  return (
    <a href={`/api/v1/flux/${chemin}`} type="application/rss+xml" target="_blank" rel="noreferrer" title={titre}
      className={`inline-flex items-center gap-1.5 text-sm font-semibold text-corail hover:underline ${className}`}>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <circle cx="5" cy="19" r="3" />
        <path d="M2 9.5v3.6A8.9 8.9 0 0 1 10.9 22h3.6C14.5 15.1 8.9 9.5 2 9.5z" />
        <path d="M2 2v3.6C11 5.6 18.4 13 18.4 22H22C22 11 13 2 2 2z" />
      </svg>
      {t('commun.fluxRss')}
    </a>
  )
}
