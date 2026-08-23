import { Routes, Route } from 'react-router'
import Layout from './components/Layout'
import Accueil from './pages/Accueil'
import Biens from './pages/Biens'
import BienDetail from './pages/BienDetail'

// Arborescence issue de la charte (livrable 10) : accueil, biens, fiche, blog, espaces connectés.
export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Accueil />} />
        <Route path="biens" element={<Biens />} />
        <Route path="biens/:id" element={<BienDetail />} />
        <Route path="*" element={<Biens />} />
      </Route>
    </Routes>
  )
}
