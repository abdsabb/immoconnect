import { describe, expect, it } from 'vitest'
import { statutsPour, versRequete } from '../services/annonces'

describe('versRequete : du formulaire d’annonce à la requête de l’API', () => {
  const formulaire = {
    categorieId: '2', typeOffre: 'vente', titre: 'Appartement', description: 'Lumineux', prix: '289000', superficie: '92.5',
    nbChambres: '2', peb: 'B', adresse: 'Avenue Louis Bertrand 40', ville: 'Schaerbeek', codePostal: '1030',
    latitude: '50.8642', longitude: '4.3789', statut: 'disponible',
  }

  it('transmet tous les champs obligatoires de l’API, classe PEB comprise', () => {
    // Régression : la classe PEB, oubliée ici, faisait refuser tout enregistrement d’annonce
    const requete = versRequete(formulaire)
    for (const champ of ['categorieId', 'titre', 'description', 'prix', 'superficie', 'nbChambres', 'peb', 'adresse', 'ville', 'codePostal', 'latitude', 'longitude']) {
      expect(requete[champ], champ).toBeDefined()
    }
    expect(requete.peb).toBe('B')
  })

  it('convertit les champs numériques', () => {
    expect(versRequete(formulaire)).toMatchObject({ categorieId: 2, prix: 289000, superficie: 92.5, nbChambres: 2, latitude: 50.8642 })
  })

  it('un bien à vendre ne se loue pas, un bien à louer ne se vend pas', () => {
    expect(statutsPour('vente')).not.toContain('loue')
    expect(statutsPour('location')).not.toContain('vendu')
  })
})
