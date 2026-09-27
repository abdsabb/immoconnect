import { NavLink, Outlet } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { useConversations } from '../services/messages'

const LANGUES = ['fr', 'nl', 'en']

// Seconde barre, réservée au compte connecté : ses liens dépendent du rôle. La navigation
// principale garde ainsi ses quatre entrées, quelle que soit la personne connectée.
function BarreEspace({ role }) {
  const { t } = useTranslation()
  const { nonLus } = useConversations()
  const lien = ({ isActive }) =>
    `whitespace-nowrap px-3 py-2 text-sm font-semibold border-b-2 ${
      isActive ? 'border-corail text-white' : 'border-transparent text-white/80 hover:text-white'
    }`
  return (
    <nav aria-label={t('nav.profil')} className="bg-nuit border-t border-white/10">
      <div className="mx-auto max-w-6xl px-4 flex gap-1 overflow-x-auto">
        {role !== 'admin' && <NavLink to="/rendez-vous" className={lien}>{t(role === 'agent' ? 'nav.agenda' : 'nav.visites')}</NavLink>}
        {role === 'membre' && <NavLink to="/favoris" className={lien}>{t('nav.favoris')}</NavLink>}
        {role !== 'admin' && (
          <NavLink to="/messages" className={lien}>
            {t('nav.messages')}
            {nonLus > 0 && <span className="ml-2 rounded-full bg-corail text-white text-xs font-bold px-2 py-0.5">{nonLus}</span>}
          </NavLink>
        )}
        <NavLink to="/profil" className={lien}>{t('nav.monProfil')}</NavLink>
      </div>
    </nav>
  )
}

// Gabarit commun à toutes les pages : en-tête (logo, navigation à 4 entrées,
// sélecteur de langue) et pied de page — structure du site du livrable 10.
export default function Layout() {
  const { t, i18n } = useTranslation()
  const { estConnecte, utilisateur, deconnecter } = useAuth()
  const lien = ({ isActive }) =>
    `px-3 py-2 rounded-md font-titre font-semibold ${isActive ? 'text-corail' : 'text-white hover:text-turquoise'}`

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-nuit text-white">
        <div className="mx-auto max-w-6xl px-4 h-16 flex items-center justify-between gap-4">
          <NavLink to="/" className="font-titre text-2xl font-extrabold tracking-tight">
            Immo<span className="text-corail">Connect</span>
          </NavLink>
          <nav aria-label="Navigation principale" className="hidden md:flex items-center gap-1">
            <NavLink to="/" end className={lien}>{t('nav.accueil')}</NavLink>
            <NavLink to="/biens" className={lien}>{t('nav.biens')}</NavLink>
            <NavLink to="/blog" className={lien}>{t('nav.blog')}</NavLink>
            {estConnecte ? (
              <button type="button" onClick={deconnecter} className="px-3 py-2 rounded-md font-titre font-semibold text-white/80 hover:text-turquoise">
                {t('nav.deconnexion')} ({utilisateur?.prenom})
              </button>
            ) : (
              <NavLink to="/connexion" className={lien}>{t('nav.connexion')}</NavLink>
            )}
          </nav>
          <div className="flex items-center gap-1" role="group" aria-label="Langue">
            {LANGUES.map((l) => (
              <button
                key={l}
                type="button"
                onClick={() => i18n.changeLanguage(l)}
                aria-pressed={i18n.resolvedLanguage === l}
                className={`px-2 py-1 text-sm uppercase rounded ${
                  i18n.resolvedLanguage === l ? 'bg-turquoise text-white' : 'text-white/80 hover:text-white'
                }`}
              >
                {l}
              </button>
            ))}
          </div>
        </div>
        {estConnecte && <BarreEspace role={utilisateur?.role} />}
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="bg-nuit text-white/80 text-sm">
        <div className="mx-auto max-w-6xl px-4 py-6 flex flex-wrap justify-between gap-2">
          <span>© {new Date().getFullYear()} ImmoConnect — Bruxelles</span>
          <span className="flex gap-4">
            <a href="/mentions-legales" className="hover:text-white">{t('pied.mentions')}</a>
            <a href="/confidentialite" className="hover:text-white">{t('pied.confidentialite')}</a>
            <span>{t('pied.osm')}</span>
          </span>
        </div>
      </footer>
    </div>
  )
}
