import { useState } from 'react'
import { Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import { formatPrix } from '../services/biens'

// Badges de statut : couleurs sémantiques de la charte, alignées sur l'énumération StatutBien
const BADGES = {
  disponible: 'bg-white text-succes shadow',
  sous_option: 'bg-ambre text-nuit shadow',
  vendu: 'bg-white text-gray-700 shadow',
  loue: 'bg-white text-gray-700 shadow',
  archive: 'bg-white text-gray-700 shadow',
}

export function Photo({ src, alt, className }) {
  const [erreur, setErreur] = useState(!src)
  if (erreur) {
    return (
      <div className={`${className} bg-gradient-to-br from-nuit to-turquoise flex items-center justify-center text-white/80`} aria-label={alt}>
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
          <path d="M3 11.5 12 4l9 7.5" /><path d="M5 10v10h14V10" /><path d="M10 20v-6h4v6" />
        </svg>
      </div>
    )
  }
  return <img src={src} alt={alt} className={`${className} object-cover`} loading="lazy" onError={() => setErreur(true)} />
}

// Carte d'un bien dans la liste (gabarit « rubrique », maquette Figure 14)
export default function CarteBien({ bien }) {
  const { t } = useTranslation()
  return (
    <article className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm hover:shadow-md transition-shadow flex flex-col">
      <Link to={`/biens/${bien.id}`} className="block relative">
        <Photo src={bien.photoCouverture} alt={bien.titre} className="w-full h-48" />
        <span className={`absolute top-3 left-3 text-xs font-semibold px-2 py-1 rounded-full ${BADGES[bien.statut] ?? BADGES.archive}`}>
          {t(`statut.${bien.statut}`)}
        </span>
      </Link>
      <div className="p-4 flex-1 flex flex-col gap-2">
        <p className="text-xs uppercase tracking-wide text-turquoise font-semibold">{bien.categorie} · {bien.ville}</p>
        <h2 className="font-titre font-bold text-nuit leading-snug">
          <Link to={`/biens/${bien.id}`} className="hover:text-turquoise">{bien.titre}</Link>
        </h2>
        <p className="text-sm text-gray-600">
          {bien.superficie} m² · {bien.nbChambres} {t('bien.chambres', { count: bien.nbChambres })}
        </p>
        <p className="mt-auto font-titre text-xl font-extrabold text-corail">{formatPrix(bien.prix)}</p>
      </div>
    </article>
  )
}
