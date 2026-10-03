import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { rendre } from './rendu'
import Blog from '../pages/Blog'
import ContenuArticle, { blocs } from '../components/ContenuArticle'

const article = (id, titre, imageUrl) => ({
  id, titre, extrait: `Chapeau de l’article ${id}.`, imageUrl, minutesDeLecture: 2, categorieId: 1, categorie: 'Conseils achat',
  statut: 'publie', publieLe: '2026-09-12', auteur: 'Yasmine Benali',
})
vi.mock('../services/admin', async (original) => ({
  ...(await original()),
  chargerCategoriesBlog: () => Promise.resolve([{ id: 1, nom: 'Conseils achat' }]),
  chargerArticles: () => Promise.resolve({
    contenu: [article(7, 'Acheter à Ixelles', '/storage/articles/7/couverture.jpg'), article(8, 'Louer à Uccle', null)],
    page: 0, taille: 10, totalElements: 2, totalPages: 1,
  }),
}))

describe('Blog (cas V5)', () => {
  it('met le premier article à la une et illustre les cartes', async () => {
    const { container } = rendre(<Blog />)
    expect(await screen.findByRole('link', { name: 'Acheter à Ixelles' })).toHaveAttribute('href', '/blog/7')
    expect(screen.getByText('À la une')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Louer à Uccle' })).toHaveAttribute('href', '/blog/8')
    // L'article illustré montre sa couverture ; l'autre, l'image de remplacement
    expect(container.querySelectorAll('img')).toHaveLength(1)
    expect(container.querySelector('img')).toHaveAttribute('src', '/storage/articles/7/couverture.jpg')
    expect(screen.getAllByText(/2 min de lecture/)).toHaveLength(2)
  })
})

describe('Contenu d’un article', () => {
  const texte = 'Chapeau.\n\n## Le budget\n\n- Notaire\n- Crédit\n\nSuite <script>alert(1)</script>\nsur deux lignes.'

  it('reconnaît paragraphes, intertitres et listes', () => {
    expect(blocs(texte)).toEqual([
      { type: 'paragraphe', texte: 'Chapeau.' },
      { type: 'titre', texte: 'Le budget' },
      { type: 'liste', elements: ['Notaire', 'Crédit'] },
      { type: 'paragraphe', texte: 'Suite <script>alert(1)</script>\nsur deux lignes.' },
    ])
    expect(blocs(null)).toEqual([])
  })

  it('n’interprète aucune balise saisie par le rédacteur', () => {
    const { container } = render(<ContenuArticle contenu={texte} />)
    expect(screen.getByRole('heading', { level: 2, name: 'Le budget' })).toBeInTheDocument()
    expect(screen.getAllByRole('listitem')).toHaveLength(2)
    expect(container.querySelector('script')).toBeNull()
    expect(screen.getByText(/<script>alert\(1\)<\/script>/)).toBeInTheDocument()
  })
})
