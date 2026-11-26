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

const UNE_HEURE_MS = DUREE_VISITE * 60 * 1000
const DEBUT_SOIREE = 18 * 60
const OCCUPE = ['demande', 'confirme', 'honore']

/**
 * Créneaux d'une plage que personne n'a réservés, pour l'agenda de l'agent : ceux qu'aucune visite demandée,
 * confirmée ou effectuée n'occupe. Une visite annulée libère son créneau. Un créneau dont l'heure est passée
 * est signalé (passe) : il n'est plus réservable.
 * Renvoie des dates à l'heure de l'agence, sans fuseau : { debut: « 2026-12-01T09:00:00 », fin: « …T10:00:00 », type, passe }.
 * Le type suit la règle du serveur : premium en soirée (à partir de 18 h) et le week-end, standard sinon.
 */
export function creneauxNonReserves(plage, rendezVous, maintenant = new Date()) {
  const jour = plage.debut.slice(0, 10)
  const weekEnd = [0, 6].includes(new Date(plage.debut).getDay())
  const type = (heure) => (weekEnd || enMinutes(heure) >= DEBUT_SOIREE ? 'premium' : 'standard')
  const visites = rendezVous.filter((rdv) => OCCUPE.includes(rdv.statut)).map((rdv) => new Date(rdv.dateHeure).getTime())
  return creneauxDeLaPlage(plage.debut.slice(11, 16), plage.fin.slice(11, 16))
    .map((heure) => ({ debut: `${jour}T${heure}:00`, fin: `${jour}T${enHeure(enMinutes(heure) + DUREE_VISITE)}:00`, type: type(heure) }))
    .map((creneau) => ({ ...creneau, passe: new Date(creneau.debut) <= maintenant }))
    .filter((creneau) => !visites.some((visite) => Math.abs(visite - new Date(creneau.debut).getTime()) < UNE_HEURE_MS))
}

/** Heures proposées dans les listes du formulaire, par demi-heure, entre deux bornes incluses. */
export function demiHeures(de = OUVERTURE, a = FERMETURE) {
  const heures = []
  for (let minute = de; minute <= a; minute += 30) heures.push(enHeure(minute))
  return heures
}
