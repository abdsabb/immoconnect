import { useTranslation } from 'react-i18next'

// Couleurs de l'étiquette énergétique régionale, du vert foncé (A++) au rouge (G)
const COULEURS = {
  'A++': 'bg-[#00843d] text-white', 'A+': 'bg-[#1a9c3f] text-white', A: 'bg-[#4cb748] text-white',
  B: 'bg-[#9ccc3d] text-nuit', C: 'bg-[#f2e93b] text-nuit', D: 'bg-[#f4b731] text-nuit',
  E: 'bg-[#ef8b2c] text-white', F: 'bg-[#e8552f] text-white', G: 'bg-[#c8102e] text-white',
}

// Classe PEB d'un bien, obligatoire dans toute publicité immobilière (chapitre 11 du rapport) :
// affichée sur chaque carte et chaque fiche.
export default function EtiquettePeb({ classe, grande = false, className = '' }) {
  const { t } = useTranslation()
  if (!classe) return null
  return (
    <span className={`inline-flex items-center gap-1 rounded font-titre font-extrabold ${grande ? 'px-3 py-1 text-lg' : 'px-2 py-0.5 text-xs'} ${COULEURS[classe] ?? 'bg-gray-200 text-nuit'} ${className}`}
      title={t('bien.pebTitre', { classe })}>
      <span className={`${grande ? 'text-sm' : 'text-[10px]'} font-semibold opacity-90`}>{t('bien.peb')}</span>
      <span>{classe}</span>
    </span>
  )
}

export const CLASSES_PEB = Object.keys(COULEURS)
