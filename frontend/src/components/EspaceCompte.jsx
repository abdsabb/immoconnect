import { NavLink, Outlet } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { useConversations } from '../services/messages'

// Rubriques du back-office de l'administrateur : une par cas d'utilisation (A1 à A8) et par outil de supervision
const RUBRIQUES_ADMIN = [
  { chemin: '/admin', cle: 'statistiques', fin: true },
  { chemin: '/admin/utilisateurs', cle: 'comptes' },
  { chemin: '/admin/annonces', cle: 'annonces' },
  { chemin: '/admin/messagerie', cle: 'messagerie' },
  { chemin: '/admin/contacts', cle: 'contacts' },
  { chemin: '/admin/journal', cle: 'journal' },
  { chemin: '/admin/articles', cle: 'articles' },
  { chemin: '/admin/categories', cle: 'categories' },
  { chemin: '/admin/traductions', cle: 'traductions' },
  { chemin: '/admin/cles-api', cle: 'clesApi' },
  { chemin: '/admin/parametres', cle: 'parametres' },
  { chemin: '/admin/securite', cle: 'securite' },
]

/** Les liens de l'espace connecté, selon le rôle. */
function liensPour(role, t) {
  if (role === 'admin') {
    return [
      ...RUBRIQUES_ADMIN.map((r) => ({ chemin: r.chemin, fin: r.fin, libelle: t(`admin.rubrique.${r.cle}`) })),
      { chemin: '/profil', libelle: t('nav.monProfil') },
    ]
  }
  if (role === 'agent') {
    return [
      { chemin: '/annonces', libelle: t('nav.annonces') },
      { chemin: '/rendez-vous', libelle: t('nav.agenda') },
      { chemin: '/messages', libelle: t('nav.messages'), messages: true },
      { chemin: '/profil', libelle: t('nav.monProfil') },
    ]
  }
  return [
    { chemin: '/rendez-vous', libelle: t('nav.visites') },
    { chemin: '/favoris', libelle: t('nav.favoris') },
    { chemin: '/messages', libelle: t('nav.messages'), messages: true },
    { chemin: '/profil', libelle: t('nav.monProfil') },
  ]
}

/**
 * Gabarit des espaces connectés (membre, agent, administrateur) : le menu de l'espace à gauche, la page à
 * droite. Sur téléphone, le menu passe au-dessus de la page et défile horizontalement.
 */
export default function EspaceCompte() {
  const { t } = useTranslation()
  const { utilisateur } = useAuth()
  const { nonLus } = useConversations()
  const role = utilisateur?.role
  const lien = ({ isActive }) =>
    `flex items-center justify-between gap-2 whitespace-nowrap rounded-lg px-3 py-2 text-sm font-titre font-semibold ${
      isActive ? 'bg-nuit text-white' : 'text-nuit hover:bg-perle'}`

  return (
    <div className="mx-auto max-w-7xl lg:grid lg:grid-cols-[15rem_minmax(0,1fr)] lg:gap-2 lg:px-4">
      <aside className="border-b border-gray-200 bg-white lg:border-b-0 lg:border-r lg:py-10 lg:pr-4">
        <nav aria-label={t('nav.profil')} className="lg:sticky lg:top-6">
          <p className="hidden px-3 pb-3 lg:block">
            <span className="block font-titre font-bold text-nuit">{utilisateur?.prenom} {utilisateur?.nom}</span>
            <span className="text-xs uppercase tracking-wide text-gray-500">{t(`admin.role.${role}`, { defaultValue: role })}</span>
          </p>
          <ul className="flex gap-1 overflow-x-auto px-4 py-2 lg:flex-col lg:overflow-visible lg:p-0">
            {liensPour(role, t).map((l) => (
              <li key={l.chemin}>
                <NavLink to={l.chemin} end={l.fin} className={lien}>
                  {l.libelle}
                  {l.messages && nonLus > 0 && <span className="rounded-full bg-corail px-2 py-0.5 text-xs font-bold text-white">{nonLus}</span>}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </aside>
      <div className="min-w-0">
        <Outlet />
      </div>
    </div>
  )
}
