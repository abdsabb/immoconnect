import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import i18n from 'i18next'
import { Champ, erreursApi, reglesMotDePasse } from '../components/Formulaire'
import EtiquettePeb, { CLASSES_PEB } from '../components/EtiquettePeb'

const t = i18n.t.bind(i18n)

describe('règles du mot de passe (miroir de la règle serveur)', () => {
  const { validate } = reglesMotDePasse(t)

  it('exige huit caractères', () => {
    expect(validate('Ab1')).toBe(t('auth.motDePasseCourt'))
  })

  it('exige minuscules et majuscules', () => {
    expect(validate('visitebxl2026')).toBe(t('auth.motDePasseCasse'))
    expect(validate('VISITEBXL2026')).toBe(t('auth.motDePasseCasse'))
  })

  it('exige un chiffre', () => {
    expect(validate('Visite-Bruxelles')).toBe(t('auth.motDePasseChiffre'))
  })

  it('accepte un mot de passe conforme', () => {
    expect(validate('Visite-Bxl-2026')).toBe(true)
  })
})

describe('erreursApi : réponse problem+json vers message et champs', () => {
  it('sans réponse, signale une erreur réseau', () => {
    expect(erreursApi(new Error('réseau'), t)).toEqual({ message: t('commun.erreurReseau'), champs: {}, type: null })
  })

  it('traduit le type connu et renvoie les champs invalides', () => {
    const erreur = { response: { data: { type: 'https://www.immoconnect.be/erreurs/trop-de-tentatives', detail: 'x', champs: { email: 'adresse invalide' } } } }
    expect(erreursApi(erreur, t)).toEqual({ message: t('erreur.trop-de-tentatives'), champs: { email: 'adresse invalide' }, type: 'trop-de-tentatives' })
  })

  it('affiche le détail de l’API pour un type inconnu', () => {
    const erreur = { response: { data: { type: 'https://www.immoconnect.be/erreurs/inconnu-du-front', detail: 'Ce créneau vient d’être réservé.' } } }
    expect(erreursApi(erreur, t).message).toBe('Ce créneau vient d’être réservé.')
  })
})

describe('Champ', () => {
  it('relie le libellé au champ et annonce l’erreur', () => {
    render(<Champ label="Adresse e-mail" erreur="obligatoire"><input /></Champ>)
    expect(screen.getByLabelText(/Adresse e-mail/)).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('obligatoire')
  })
})

describe('EtiquettePeb', () => {
  it('connaît les neuf classes, de A++ à G', () => {
    expect(CLASSES_PEB).toEqual(['A++', 'A+', 'A', 'B', 'C', 'D', 'E', 'F', 'G'])
  })

  it('affiche la classe avec son intitulé accessible', () => {
    render(<EtiquettePeb classe="B" />)
    expect(screen.getByTitle(/classe B/)).toHaveTextContent('PEB')
    expect(screen.getByTitle(/classe B/)).toHaveTextContent('B')
  })

  it('n’affiche rien sans classe', () => {
    const { container } = render(<EtiquettePeb classe={null} />)
    expect(container).toBeEmptyDOMElement()
  })
})
