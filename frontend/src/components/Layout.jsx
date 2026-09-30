import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { useConversations } from '../services/messages'
import { useConfiguration } from '../services/configuration'

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
        {role === 'agent' && <NavLink to="/annonces" className={lien}>{t('nav.annonces')}</NavLink>}
        {role !== 'admin' && <NavLink to="/rendez-vous" className={lien}>{t(role === 'agent' ? 'nav.agenda' : 'nav.visites')}</NavLink>}
        {role === 'membre' && <NavLink to="/favoris" className={lien}>{t('nav.favoris')}</NavLink>}
        {role !== 'admin' && (
          <NavLink to="/messages" className={lien}>
            {t('nav.messages')}
            {nonLus > 0 && <span className="ml-2 rounded-full bg-corail text-white text-xs font-bold px-2 py-0.5">{nonLus}</span>}
          </NavLink>
        )}
        {role === 'admin' && <NavLink to="/admin" className={lien}>{t('nav.administration')}</NavLink>}
        <NavLink to="/profil" className={lien}>{t('nav.monProfil')}</NavLink>
      </div>
    </nav>
  )
}

/**
 * Entrée « Biens » de la navigation : un lien vers tout le catalogue, avec un sous-menu
 * « À vendre » / « À louer ». Sur grand écran, le sous-menu s'ouvre au survol, au clic ou au
 * clavier (Entrée, Espace, flèche bas ; Échap le ferme) ; sur téléphone, les trois liens sont
 * simplement listés.
 */
function MenuBiens({ lien, contenu, fermer, mobile }) {
  const { t } = useTranslation()
  const [ouvert, setOuvert] = useState(false)
  const conteneur = useRef(null)
  const location = useLocation()
  const sousLiens = [['/a-vendre', contenu('nav.vente', 'offre.vente')], ['/a-louer', contenu('nav.location', 'offre.location')]]
  // Un clic hors du menu le referme ; un lien choisi aussi
  useEffect(() => {
    if (!ouvert) return undefined
    const clicDehors = (e) => { if (!conteneur.current?.contains(e.target)) setOuvert(false) }
    document.addEventListener('pointerdown', clicDehors)
    return () => document.removeEventListener('pointerdown', clicDehors)
  }, [ouvert])

  if (mobile) {
    return (
      <>
        <NavLink to="/biens" className={lien} onClick={fermer}>{contenu('nav.biens', 'nav.biens')}</NavLink>
        {sousLiens.map(([chemin, libelle]) => (
          <NavLink key={chemin} to={chemin} className={(etat) => `${lien(etat)} pl-8`} onClick={fermer}>{libelle}</NavLink>
        ))}
      </>
    )
  }
  const actif = ['/biens', '/a-vendre', '/a-louer'].some((c) => location.pathname.startsWith(c))
  return (
    <div ref={conteneur} className="relative" onMouseEnter={() => setOuvert(true)} onMouseLeave={() => setOuvert(false)}
      onKeyDown={(e) => { if (e.key === 'Escape') setOuvert(false) }}>
      <NavLink to="/biens" className={() => lien({ isActive: actif })} aria-haspopup="menu" aria-expanded={ouvert}
        onKeyDown={(e) => { if (e.key === 'ArrowDown' || e.key === ' ') { e.preventDefault(); setOuvert(true) } }}>
        {contenu('nav.biens', 'nav.biens')}
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" aria-hidden="true" className="ml-1 inline">
          <path d="M6 9l6 6 6-6" />
        </svg>
      </NavLink>
      {ouvert && (
        <ul role="menu" aria-label={t('nav.biens')} className="absolute left-0 top-full z-20 min-w-44 rounded-md bg-nuit py-1 shadow-lg ring-1 ring-white/10">
          {sousLiens.map(([chemin, libelle]) => (
            <li key={chemin} role="none">
              <NavLink role="menuitem" to={chemin} className={(etat) => `block ${lien(etat)}`} onClick={() => setOuvert(false)}>{libelle}</NavLink>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

// Gabarit commun à toutes les pages : en-tête (logo, navigation à quatre entrées — Accueil, Biens,
// Blog, Connexion —, sélecteur de langue) et pied de page, structure du site du livrable 10.
// L'entrée « Biens » mène au catalogue et déplie « À vendre » et « À louer ».
export default function Layout() {
  const { t, i18n } = useTranslation()
  const { estConnecte, utilisateur, deconnecter } = useAuth()
  const naviguer = useNavigate()
  // Identité de l'agence et langues actives, réglées par l'administrateur (cas A5 et A6)
  const site = useConfiguration().data?.site
  // À chaque changement de page, l'onglet reprend le nom du site ; la page affichée pose ensuite son propre titre
  const { pathname } = useLocation()
  const premierRendu = useRef(true)
  useLayoutEffect(() => {
    // La page d'arrivée porte déjà le titre donné par le serveur : on n'y touche pas
    if (premierRendu.current) { premierRendu.current = false; return }
    document.title = site?.nom ?? 'ImmoConnect'
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pathname])
  const langues = LANGUES.filter((l) => !site?.languesActives || site.languesActives.includes(l))
  useEffect(() => {
    // Une langue désactivée par l'administrateur ne reste pas affichée : retour au français
    if (site?.languesActives && !site.languesActives.includes(i18n.resolvedLanguage)) i18n.changeLanguage('fr')
  }, [site, i18n])
  // Textes du site gérés par l'administrateur (cas A6) ; à défaut, le texte livré avec l'interface
  const contenu = (cle, repli) => t(`contenu:${cle}`, { defaultValue: t(repli) })
  // Sur téléphone, la navigation se replie derrière un bouton ; un lien choisi la referme
  const [menuOuvert, setMenuOuvert] = useState(false)
  const fermer = () => setMenuOuvert(false)
  const lien = ({ isActive }) =>
    `px-3 py-2 rounded-md font-titre font-semibold ${isActive ? 'text-corail' : 'text-white hover:text-turquoise'}`
  const liens = (mobile) => (
    <>
      <NavLink to="/" end className={lien} onClick={fermer}>{contenu('nav.accueil', 'nav.accueil')}</NavLink>
      <MenuBiens lien={lien} contenu={contenu} fermer={fermer} mobile={mobile} />
      <NavLink to="/blog" className={lien} onClick={fermer}>{contenu('nav.blog', 'nav.blog')}</NavLink>
      {estConnecte ? (
        <button type="button" onClick={() => { fermer(); deconnecter().then(() => naviguer('/')) }}
          className="px-3 py-2 rounded-md text-left font-titre font-semibold text-white/80 hover:text-turquoise">
          {t('nav.deconnexion')} ({utilisateur?.prenom})
        </button>
      ) : (
        <NavLink to="/connexion" className={lien} onClick={fermer}>{contenu('nav.connexion', 'nav.connexion')}</NavLink>
      )}
    </>
  )

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-nuit text-white">
        <div className="mx-auto max-w-6xl px-4 h-16 flex items-center justify-between gap-4">
          <NavLink to="/" onClick={fermer} className="font-titre text-2xl font-extrabold tracking-tight">
            Immo<span className="text-corail">Connect</span>
          </NavLink>
          <nav aria-label="Navigation principale" className="hidden md:flex items-center gap-1">{liens(false)}</nav>
          <div className="flex items-center gap-1" role="group" aria-label="Langue">
            {langues.map((l) => (
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
            <button type="button" onClick={() => setMenuOuvert(!menuOuvert)} aria-expanded={menuOuvert} aria-controls="menu-mobile"
              aria-label={t('nav.menu')} className="ml-1 rounded p-2 text-white hover:text-turquoise md:hidden">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
                {menuOuvert ? <path d="M6 6l12 12M18 6L6 18" /> : <path d="M4 7h16M4 12h16M4 17h16" />}
              </svg>
            </button>
          </div>
        </div>
        {menuOuvert && (
          <nav id="menu-mobile" aria-label="Navigation principale" className="flex flex-col border-t border-white/10 px-4 py-2 md:hidden">{liens(true)}</nav>
        )}
        {estConnecte && <BarreEspace role={utilisateur?.role} />}
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="bg-nuit text-white/80 text-sm">
        <div className="mx-auto max-w-6xl px-4 py-6 flex flex-wrap justify-between gap-2">
          <span>
            © {new Date().getFullYear()} {site?.nom ?? 'ImmoConnect'}
            {site?.adresse && <span className="hidden sm:inline"> — {site.adresse}</span>}
            {site?.telephone && <span className="hidden sm:inline"> · <a href={`tel:${site.telephone.replace(/ /g, '')}`} className="hover:text-white">{site.telephone}</a></span>}
          </span>
          <span className="flex gap-4">
            {/* Liens internes : un lien ordinaire rechargerait la page et fermerait la session, gardée en mémoire */}
            <NavLink to="/mentions-legales" className="hover:text-white">{contenu('footer.mentions', 'pied.mentions')}</NavLink>
            <NavLink to="/confidentialite" className="hover:text-white">{contenu('footer.rgpd', 'pied.confidentialite')}</NavLink>
            <NavLink to="/conditions" className="hover:text-white">{t('pied.conditions')}</NavLink>
            <NavLink to="/credits-photos" className="hover:text-white">{t('pied.credits')}</NavLink>
            <span>{t('pied.osm')}</span>
          </span>
        </div>
      </footer>
    </div>
  )
}
