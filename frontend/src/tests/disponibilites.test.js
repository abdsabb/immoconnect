import { describe, expect, it } from 'vitest'
import { creneauxDeLaPlage, demiHeures, enHeure } from '../services/disponibilites'

// Miroir de la règle serveur (GrilleCreneaux.decouper) : l'aperçu montré à l'agent doit annoncer
// exactement les créneaux que le membre verra.
describe('créneaux offerts par une plage de disponibilité', () => {
  it('découpe une matinée en visites d’une heure espacées d’une demi-heure', () => {
    expect(creneauxDeLaPlage('09:00', '12:00')).toEqual(['09:00', '10:30'])
  })

  it('remplit une après-midi et une soirée jusqu’à la fermeture', () => {
    expect(creneauxDeLaPlage('14:00', '21:00')).toEqual(['14:00', '15:30', '17:00', '18:30', '20:00'])
  })

  it('part de l’heure choisie par l’agent', () => {
    expect(creneauxDeLaPlage('13:30', '16:00')).toEqual(['13:30', '15:00'])
  })

  it('n’offre rien si la plage est plus courte qu’une visite', () => {
    expect(creneauxDeLaPlage('14:00', '14:30')).toEqual([])
    expect(creneauxDeLaPlage('14:00', '15:00')).toEqual(['14:00'])
    expect(creneauxDeLaPlage('15:00', '14:00')).toEqual([])
  })
})

describe('heures proposées dans le formulaire', () => {
  it('va de l’ouverture à la fermeture de l’agence, par demi-heure', () => {
    const heures = demiHeures()
    expect(heures[0]).toBe('08:00')
    expect(heures[1]).toBe('08:30')
    expect(heures.at(-1)).toBe('21:00')
    expect(heures).toHaveLength(27)
  })

  it('écrit les heures sur deux chiffres', () => {
    expect(enHeure(9 * 60 + 30)).toBe('09:30')
  })
})
