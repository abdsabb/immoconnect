import { Routes, Route } from 'react-router'
import Layout from './components/Layout'
import RouteProtegee from './auth/RouteProtegee'
import Accueil from './pages/Accueil'
import Biens from './pages/Biens'
import BienDetail from './pages/BienDetail'
import Connexion from './pages/Connexion'
import Inscription from './pages/Inscription'
import Profil from './pages/Profil'
import PriseRendezVous from './pages/PriseRendezVous'
import MesRendezVous from './pages/MesRendezVous'
import MesFavoris from './pages/MesFavoris'
import Messagerie from './pages/Messagerie'
import MesAnnonces from './pages/MesAnnonces'
import AnnonceFormulaire from './pages/AnnonceFormulaire'
import Blog from './pages/Blog'
import Article from './pages/Article'
import CreditsPhotos from './pages/CreditsPhotos'
import PageLegale from './pages/PageLegale'
import MotDePasseOublie from './pages/MotDePasseOublie'
import Activation from './pages/Activation'
import Administration from './pages/admin/Administration'
import TableauDeBord from './pages/admin/TableauDeBord'
import Comptes from './pages/admin/Comptes'
import Journal from './pages/admin/Journal'
import Articles from './pages/admin/Articles'
import ArticleFormulaire from './pages/admin/ArticleFormulaire'
import Categories from './pages/admin/Categories'
import Traductions from './pages/admin/Traductions'
import ClesApi from './pages/admin/ClesApi'
import Signalements from './pages/admin/Signalements'
import Parametres from './pages/admin/Parametres'

// Arborescence issue de la charte (livrable 10) : pages publiques, connexion/inscription,
// espace membre, espace agent, back-office administrateur.
export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Accueil />} />
        <Route path="a-vendre" element={<Biens typeOffre="vente" />} />
        <Route path="a-louer" element={<Biens typeOffre="location" />} />
        <Route path="biens" element={<Biens />} />
        <Route path="biens/:id" element={<BienDetail />} />
        <Route path="blog" element={<Blog />} />
        <Route path="blog/:id" element={<Article />} />
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
            <Route path="signalements" element={<Signalements />} />
            <Route path="parametres" element={<Parametres />} />
          </Route>
        </Route>
        <Route path="*" element={<Biens />} />
      </Route>
    </Routes>
  )
}
