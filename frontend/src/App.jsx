import { Routes, Route } from 'react-router'
import Layout from './components/Layout'
import RouteProtegee from './auth/RouteProtegee'
import Accueil from './pages/Accueil'
import Biens from './pages/Biens'
import BienDetail from './pages/BienDetail'
import Connexion from './pages/Connexion'
import Inscription from './pages/Inscription'
import Profil from './pages/Profil'

// Arborescence issue de la charte (livrable 10) : pages publiques, connexion/inscription, espace membre.
export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Accueil />} />
        <Route path="biens" element={<Biens />} />
        <Route path="biens/:id" element={<BienDetail />} />
        <Route path="connexion" element={<Connexion />} />
        <Route path="inscription" element={<Inscription />} />
        <Route element={<RouteProtegee />}>
          <Route path="profil" element={<Profil />} />
        </Route>
        <Route path="*" element={<Biens />} />
      </Route>
    </Routes>
  )
}
