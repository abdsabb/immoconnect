import { Routes, Route } from 'react-router'
import Layout from './components/Layout'
import Accueil from './pages/Accueil'
import Biens from './pages/Biens'

// Arborescence issue de la charte (livrable 10) : accueil, biens, blog, espaces connectés.
export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Accueil />} />
        <Route path="biens" element={<Biens />} />
        <Route path="*" element={<Biens />} />
      </Route>
    </Routes>
  )
}
