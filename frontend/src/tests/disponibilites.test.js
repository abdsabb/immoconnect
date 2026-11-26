import { describe, expect, it } from 'vitest'
import { creneauxDeLaPlage, creneauxNonReserves, demiHeures, enHeure } from '../services/disponibilites'

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

describe('créneaux non réservés écrits dans l’agenda de l’agent', () => {
  const plage = { debut: '2026-12-01T09:00:00', fin: '2026-12-01T12:00:00' }
  const laVeille = new Date('2026-11-30T12:00:00')

  it('écrit chaque créneau de la plage avec son heure de début et de fin', () => {
    expect(creneauxNonReserves(plage, [], laVeille)).toEqual([
      { debut: '2026-12-01T09:00:00', fin: '2026-12-01T10:00:00', type: 'standard', passe: false },
      { debut: '2026-12-01T10:30:00', fin: '2026-12-01T11:30:00', type: 'standard', passe: false },
    ])
  })

  it('marque premium les créneaux du soir et du week-end, comme le serveur', () => {
    const soiree = { debut: '2026-12-01T16:30:00', fin: '2026-12-01T21:00:00' }
    expect(creneauxNonReserves(soiree, [], laVeille).map((c) => `${c.debut.slice(11, 16)} ${c.type}`))
      .toEqual(['16:30 standard', '18:00 premium', '19:30 premium'])
    const samedi = { debut: '2026-12-05T10:00:00', fin: '2026-12-05T12:00:00' }
    expect(creneauxNonReserves(samedi, [], laVeille).map((c) => c.type)).toEqual(['premium'])
  })

  it('retire le créneau d’une visite demandée ou confirmée', () => {
    const demande = [{ statut: 'demande', dateHeure: '2026-12-01T09:00:00' }]
    expect(creneauxNonReserves(plage, demande, laVeille).map((c) => c.debut)).toEqual(['2026-12-01T10:30:00'])
    const confirme = [{ statut: 'confirme', dateHeure: '2026-12-01T10:30:00' }]
    expect(creneauxNonReserves(plage, confirme, laVeille).map((c) => c.debut)).toEqual(['2026-12-01T09:00:00'])
  })

  it('rend le créneau d’une visite annulée', () => {
    const annule = [{ statut: 'annule', dateHeure: '2026-12-01T09:00:00' }]
    expect(creneauxNonReserves(plage, annule, laVeille)).toHaveLength(2)
  })

  it('signale le créneau dont l’heure est passée : il n’est plus libre, il n’a pas été réservé', () => {
    const aDixHeures = new Date('2026-12-01T10:00:00')
    expect(creneauxNonReserves(plage, [], aDixHeures).map((c) => c.passe)).toEqual([true, false])
    expect(creneauxNonReserves(plage, [], new Date('2026-12-01T13:00:00')).map((c) => c.passe)).toEqual([true, true])
  })

  it('ne dit pas « non réservé » d’un créneau où la visite a eu lieu', () => {
    const honore = [{ statut: 'honore', dateHeure: '2026-12-01T09:00:00' }]
    expect(creneauxNonReserves(plage, honore, new Date('2026-12-01T13:00:00')).map((c) => c.debut)).toEqual(['2026-12-01T10:30:00'])
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
