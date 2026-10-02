import { Outlet } from 'react-router'
import { useTranslation } from 'react-i18next'

// Gabarit du back-office de l'administrateur (contrainte de l'épreuve) : une rubrique par cas d'utilisation A1 à A8.
// Les rubriques sont dans le menu de gauche de l'espace connecté (EspaceCompte).
export default function Administration() {
  const { t } = useTranslation()
  return (
    <section className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-3xl font-bold text-nuit">{t('admin.titre')}</h1>
      <div className="mt-6">
        <Outlet />
      </div>
    </section>
  )
}

/** Message de réussite ou d'erreur, commun aux rubriques. */
export function Avis({ message, erreur }) {
  if (erreur) return <p role="alert" className="rounded-lg bg-erreur/10 text-erreur px-4 py-3">{erreur}</p>
  if (message) return <p role="status" className="rounded-lg bg-succes/10 text-succes px-4 py-3">{message}</p>
  return null
}

export function Pagination({ page, total, onChange }) {
  const { t } = useTranslation()
  if (!total || total <= 1) return null
  return (
    <nav className="mt-6 flex items-center justify-center gap-3" aria-label="Pagination">
      <button type="button" disabled={page === 0} onClick={() => onChange(page - 1)}
        className="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-40">‹</button>
      <span className="text-sm text-gray-600">{t('commun.page', { page: page + 1, total })}</span>
      <button type="button" disabled={page >= total - 1} onClick={() => onChange(page + 1)}
        className="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-40">›</button>
    </nav>
  )
}

export const classeTableau = 'w-full text-sm border border-gray-200 rounded-xl overflow-hidden'
export const classeEnTete = 'bg-perle text-left text-xs uppercase text-gray-500 [&>th]:px-3 [&>th]:py-2'
export const classeLigne = 'border-t border-gray-200 [&>td]:px-3 [&>td]:py-2 align-top'
export const classeBouton = 'rounded-lg px-3 py-2 text-sm font-titre font-semibold disabled:opacity-60'
