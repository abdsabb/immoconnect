import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { rendre } from './rendu'
import Connexion from '../pages/Connexion'

const connecter = vi.fn()
const validerCode = vi.fn()
vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ connecter, validerCode }) }))

describe('Connexion (cas M9) et second facteur', () => {
  it('exige e-mail et mot de passe', async () => {
    const utilisateur = userEvent.setup()
    rendre(<Connexion />)
    await utilisateur.click(screen.getByRole('button', { name: /se connecter/i }))
    expect(await screen.findAllByRole('alert')).toHaveLength(2)
    expect(connecter).not.toHaveBeenCalled()
  })

  it('passe à l’étape du code quand l’API renvoie un défi, avec un champ vide', async () => {
    connecter.mockResolvedValueOnce({ doubleFacteur: true, defi: 'defi-1' })
    const utilisateur = userEvent.setup()
    rendre(<Connexion />)
    await utilisateur.type(screen.getByLabelText(/adresse e-mail/i), 'sarah.dubois@mail.be')
    await utilisateur.type(screen.getByLabelText(/mot de passe/i), 'password')
    await utilisateur.click(screen.getByRole('button', { name: /se connecter/i }))
    const champCode = await screen.findByLabelText(/code reçu/i)
    expect(champCode).toHaveValue('')
    expect(screen.getByText(/sarah.dubois@mail.be/)).toBeInTheDocument()

    await utilisateur.type(champCode, '12')
    await utilisateur.click(screen.getByRole('button', { name: /valider/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/six chiffres/i)
    expect(validerCode).not.toHaveBeenCalled()

    await utilisateur.clear(champCode)
    await utilisateur.type(champCode, '482913')
    await utilisateur.click(screen.getByRole('button', { name: /valider/i }))
    expect(validerCode).toHaveBeenCalledWith('defi-1', '482913')
  })

  it('affiche l’erreur de l’API et propose de renvoyer l’activation', async () => {
    connecter.mockRejectedValueOnce({ response: { data: { type: 'https://www.immoconnect.be/erreurs/compte-non-active', detail: 'x' } } })
    const utilisateur = userEvent.setup()
    rendre(<Connexion />)
    await utilisateur.type(screen.getByLabelText(/adresse e-mail/i), 'alice.benali@mail.be')
    await utilisateur.type(screen.getByLabelText(/mot de passe/i), 'password')
    await utilisateur.click(screen.getByRole('button', { name: /se connecter/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/confirmez d’abord/i)
    expect(screen.getByRole('button', { name: /renvoyer/i })).toBeInTheDocument()
  })
})
