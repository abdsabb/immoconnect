import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { rendre } from './rendu'
import Inscription from '../pages/Inscription'

// Le contexte d'authentification et la configuration publique sont remplacés : le test porte sur le formulaire
const inscrire = vi.fn()
vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ inscrire }) }))
vi.mock('../services/configuration', () => ({ useConfiguration: () => ({ data: { activationParCourriel: true, boiteDeDemonstration: '/courriels/' } }) }))

async function remplir(utilisateur, { motDePasse = 'Visite-Bxl-2026', cgu = true } = {}) {
  await utilisateur.type(screen.getByLabelText(/prénom/i), 'Test')
  await utilisateur.type(screen.getByLabelText(/^nom/i), 'Vitest')
  await utilisateur.type(screen.getByLabelText(/adresse e-mail/i), 'test@essai.be')
  await utilisateur.type(screen.getByLabelText(/^mot de passe/i), motDePasse)
  if (cgu) await utilisateur.click(screen.getByLabelText(/conditions générales/i))
}

describe('Inscription (cas M1)', () => {
  it('refuse un formulaire vide sans appeler l’API', async () => {
    const utilisateur = userEvent.setup()
    rendre(<Inscription />)
    await utilisateur.click(screen.getByRole('button', { name: /s'inscrire/i }))
    const alertes = await screen.findAllByRole('alert')
    expect(alertes.length).toBeGreaterThanOrEqual(4)
    expect(inscrire).not.toHaveBeenCalled()
  })

  it('refuse un mot de passe faible et exige les conditions générales', async () => {
    const utilisateur = userEvent.setup()
    rendre(<Inscription />)
    await remplir(utilisateur, { motDePasse: 'password', cgu: false })
    await utilisateur.click(screen.getByRole('button', { name: /s'inscrire/i }))
    expect(await screen.findByText('Mêlez minuscules et majuscules')).toBeInTheDocument()
    expect(screen.getByText('Les conditions générales doivent être acceptées.')).toBeInTheDocument()
    expect(inscrire).not.toHaveBeenCalled()
  })

  it('envoie les données conformes et annonce le courriel d’activation', async () => {
    inscrire.mockResolvedValueOnce({ activationRequise: true })
    const utilisateur = userEvent.setup()
    rendre(<Inscription />)
    await remplir(utilisateur)
    await utilisateur.click(screen.getByRole('button', { name: /s'inscrire/i }))
    await waitFor(() => expect(inscrire).toHaveBeenCalledTimes(1))
    expect(inscrire.mock.calls[0][0]).toMatchObject({ email: 'test@essai.be', motDePasse: 'Visite-Bxl-2026', cguAcceptees: true, consentementCommunications: false })
    expect(await screen.findByRole('status')).toHaveTextContent('test@essai.be')
    expect(screen.getByRole('link', { name: /boîte/i })).toHaveAttribute('href', '/courriels/')
  })
})
