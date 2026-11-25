import { api } from './api'

// Cas AG7 : l'agent ouvre et retire les plages pendant lesquelles il reçoit en visite

export async function chargerDisponibilites() {
  const { data } = await api.get('/disponibilites')
  return data
}

// plage : { debut, fin, semaines } — dates à l'heure de l'agence, sans fuseau : « 2026-12-01T09:00:00 »
export async function ouvrirDisponibilite(plage) {
  const { data } = await api.post('/disponibilites', plage)
  return data
}

// Sans annulerVisites, l'API refuse (409 « visites-prevues ») de retirer une plage où des visites sont prévues
export async function retirerDisponibilite(id, annulerVisites = false) {
  const { data } = await api.delete(`/disponibilites/${id}`, { params: { annulerVisites } })
  return data
}

export async function retirerPeriode(du, au, annulerVisites = false) {
  const { data } = await api.delete('/disponibilites', { params: { du, au, annulerVisites } })
  return data
}

// Miroir de la règle serveur (GrilleCreneaux) : une visite dure une heure, suivie d'une demi-heure de battement
const DUREE_VISITE = 60
const PAS = 90
export const OUVERTURE = 8 * 60
export const FERMETURE = 21 * 60

const enMinutes = (heure) => Number(heure.slice(0, 2)) * 60 + Number(heure.slice(3, 5))
export const enHeure = (minutes) => `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`

/** Heures de visite (« 09:00 ») qu'offre une plage de… à… : ce que le membre verra. */
export function creneauxDeLaPlage(de, a) {
  const creneaux = []
  for (let minute = enMinutes(de); minute + DUREE_VISITE <= enMinutes(a); minute += PAS) creneaux.push(enHeure(minute))
  return creneaux
}

/** Heures proposées dans les listes du formulaire, par demi-heure, entre deux bornes incluses. */
export function demiHeures(de = OUVERTURE, a = FERMETURE) {
  const heures = []
  for (let minute = de; minute <= a; minute += 30) heures.push(enHeure(minute))
  return heures
}
