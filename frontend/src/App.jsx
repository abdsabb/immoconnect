import { Suspense, lazy } from 'react'
import { Routes, Route } from 'react-router'
import { useTranslation } from 'react-i18next'
import Layout from './components/Layout'
import RouteProtegee from './auth/RouteProtegee'
import Accueil from './pages/Accueil'
import Biens from './pages/Biens'
import BienDetail from './pages/BienDetail'

// Découpage du code par route (chapitre 10) : les pages publiques les plus visitées partent avec
// l'application, les espaces connectés, le blog et les pages légales se chargent à la première visite
const Connexion = lazy(() => import('./pages/Connexion'))
const Inscription = lazy(() => import('./pages/Inscription'))
const Profil = lazy(() => import('./pages/Profil'))
const PriseRendezVous = lazy(() => import('./pages/PriseRendezVous'))
const MesRendezVous = lazy(() => import('./pages/MesRendezVous'))
const MesFavoris = lazy(() => import('./pages/MesFavoris'))
const Messagerie = lazy(() => import('./pages/Messagerie'))
const MesAnnonces = lazy(() => import('./pages/MesAnnonces'))
const AnnonceFormulaire = lazy(() => import('./pages/AnnonceFormulaire'))
const Blog = lazy(() => import('./pages/Blog'))
const Article = lazy(() => import('./pages/Article'))
const CreditsPhotos = lazy(() => import('./pages/CreditsPhotos'))
const PageLegale = lazy(() => import('./pages/PageLegale'))
const MotDePasseOublie = lazy(() => import('./pages/MotDePasseOublie'))
const Activation = lazy(() => import('./pages/Activation'))
const Contact = lazy(() => import('./pages/Contact'))
const Administration = lazy(() => import('./pages/admin/Administration'))
const TableauDeBord = lazy(() => import('./pages/admin/TableauDeBord'))
const Comptes = lazy(() => import('./pages/admin/Comptes'))
const Journal = lazy(() => import('./pages/admin/Journal'))
const Articles = lazy(() => import('./pages/admin/Articles'))
const ArticleFormulaire = lazy(() => import('./pages/admin/ArticleFormulaire'))
const Categories = lazy(() => import('./pages/admin/Categories'))
const Traductions = lazy(() => import('./pages/admin/Traductions'))
const ClesApi = lazy(() => import('./pages/admin/ClesApi'))
const AnnoncesAgents = lazy(() => import('./pages/admin/AnnoncesAgents'))
const MessagerieAgents = lazy(() => import('./pages/admin/MessagerieAgents'))
const DemandesContact = lazy(() => import('./pages/admin/DemandesContact'))
const Parametres = lazy(() => import('./pages/admin/Parametres'))
const Alertes = lazy(() => import('./pages/admin/Alertes'))

// Arborescence issue de la charte (livrable 10) : pages publiques, connexion/inscription,
// espace membre, espace agent, back-office administrateur.
function Chargement() {
  const { t } = useTranslation()
  return <p className="mx-auto max-w-3xl px-4 py-12 text-gray-500">{t('commun.chargement')}</p>
}

export default function App() {
  return (
    <Suspense fallback={<Chargement />}>
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Accueil />} />
        <Route path="a-vendre" element={<Biens typeOffre="vente" />} />
        <Route path="a-louer" element={<Biens typeOffre="location" />} />
        <Route path="biens" element={<Biens />} />
        <Route path="biens/:id" element={<BienDetail />} />
        <Route path="blog" element={<Blog />} />
        <Route path="blog/:id" element={<Article />} />
        <Route path="contact" element={<Contact />} />
        <Route path="credits-photos" element={<CreditsPhotos />} />
        <Route path="mentions-legales" element={<PageLegale key="mentions" page="mentions" />} />
        <Route path="confidentialite" element={<PageLegale key="confidentialite" page="confidentialite" />} />
        <Route path="conditions" element={<PageLegale key="conditions" page="conditions" />} />
        <Route path="connexion" element={<Connexion />} />
        <Route path="inscription" element={<Inscription />} />
        <Route path="mot-de-passe-oublie" element={<MotDePasseOublie />} />
        <Route path="reinitialisation" element={<MotDePasseOublie />} />
        <Route path="activation" element={<Activation />} />
        <Route element={<RouteProtegee />}>
          <Route path="profil" element={<Profil />} />
          <Route path="biens/:id/rendez-vous" element={<PriseRendezVous />} />
          <Route path="rendez-vous" element={<MesRendezVous />} />
          <Route path="favoris" element={<MesFavoris />} />
          <Route path="messages" element={<Messagerie />} />
          <Route path="messages/:interlocuteurId" element={<Messagerie />} />
        </Route>
        {/* Back-office de l'agent. La clé distingue création et modification : le formulaire repart de zéro. */}
        <Route element={<RouteProtegee roles={['agent']} />}>
          <Route path="annonces" element={<MesAnnonces />} />
          <Route path="annonces/nouvelle" element={<AnnonceFormulaire key="nouvelle" />} />
          <Route path="annonces/:id" element={<AnnonceFormulaire key="existante" />} />
        </Route>
        {/* Back-office de l'administrateur : une rubrique par cas d'utilisation */}
        <Route element={<RouteProtegee roles={['admin']} />}>
          <Route path="admin" element={<Administration />}>
            <Route index element={<TableauDeBord />} />
            <Route path="utilisateurs" element={<Comptes />} />
            <Route path="journal" element={<Journal />} />
            <Route path="articles" element={<Articles />} />
            <Route path="articles/nouveau" element={<ArticleFormulaire key="nouveau" />} />
            <Route path="articles/:id" element={<ArticleFormulaire key="existant" />} />
            <Route path="categories" element={<Categories />} />
            <Route path="traductions" element={<Traductions />} />
            <Route path="cles-api" element={<ClesApi />} />
            <Route path="annonces" element={<AnnoncesAgents />} />
            <Route path="annonces/:id" element={<AnnonceFormulaire key="supervision" />} />
            <Route path="messagerie" element={<MessagerieAgents />} />
            <Route path="contacts" element={<DemandesContact />} />
            <Route path="parametres" element={<Parametres />} />
            <Route path="securite" element={<Alertes />} />
          </Route>
        </Route>
        <Route path="*" element={<Biens />} />
      </Route>
    </Routes>
    </Suspense>
  )
}
