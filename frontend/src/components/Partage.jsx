import { useState } from 'react'
import { useTranslation } from 'react-i18next'

// Partage d'une annonce ou d'un article : de simples liens vers les pages de partage des réseaux, et la copie de l'adresse.
// Aucun script de réseau social n'est chargé : le visiteur n'est pas pisté tant qu'il ne clique pas.
const RESEAUX = [
  { nom: 'Facebook', url: (u) => `https://www.facebook.com/sharer/sharer.php?u=${u}` },
  { nom: 'WhatsApp', url: (u, t) => `https://wa.me/?text=${t}%20${u}` },
  { nom: 'X', url: (u, t) => `https://twitter.com/intent/tweet?url=${u}&text=${t}` },
  { nom: 'LinkedIn', url: (u) => `https://www.linkedin.com/sharing/share-offsite/?url=${u}` },
]

export default function Partage({ titre, libelle, className = '' }) {
  const { t } = useTranslation()
  const [copie, setCopie] = useState(false)
  // L'adresse partagée est celle de la fiche, sans paramètre de langue : chacun la lira dans la sienne
  const adresse = `${window.location.origin}${window.location.pathname}`
  const u = encodeURIComponent(adresse)
  const texte = encodeURIComponent(titre)

  const copier = async () => {
    try {
      await navigator.clipboard.writeText(adresse)
      setCopie(true)
      setTimeout(() => setCopie(false), 2500)
    } catch {
      // Presse-papiers refusé par le navigateur : l'adresse est proposée à la copie manuelle
      window.prompt(t('partage.copierManuel'), adresse)
    }
  }

  const bouton = 'rounded-full border border-gray-300 px-3 py-1 text-xs font-semibold text-nuit hover:border-turquoise hover:text-turquoise'
  return (
    <div className={className}>
      <p className="text-xs uppercase text-gray-500">{libelle ?? t('partage.titre')}</p>
      <div className="mt-2 flex flex-wrap items-center gap-2">
        {RESEAUX.map((r) => (
          <a key={r.nom} href={r.url(u, texte)} target="_blank" rel="noopener noreferrer" className={bouton}
            aria-label={t('partage.sur', { reseau: r.nom })}>
            {r.nom}
          </a>
        ))}
        <a href={`mailto:?subject=${texte}&body=${u}`} className={bouton}>{t('partage.courriel')}</a>
        <button type="button" onClick={copier} className={bouton}>{t(copie ? 'partage.copie' : 'partage.copier')}</button>
        <span role="status" className="sr-only">{copie ? t('partage.copie') : ''}</span>
      </div>
    </div>
  )
}
