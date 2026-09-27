import { api } from './api'

// Cas M4 / M5 / AG5 : rendez-vous de visite et paiement des créneaux premium (livrable 15, §6.5 et §6.6)

export async function chargerCreneaux(bienId) {
  const { data } = await api.get(`/biens/${bienId}/creneaux`)
  return data
}

export async function chargerRendezVous() {
  const { data } = await api.get('/rendez-vous')
  return data
}

export async function reserver(demande) {
  const { data } = await api.post('/rendez-vous', demande)
  return data
}

// action : confirmer | annuler | honorer — les transitions du diagramme d'état
export async function changerStatut(id, action) {
  const { data } = await api.patch(`/rendez-vous/${id}/${action}`)
  return data
}

export async function chargerConfigPaiement() {
  const { data } = await api.get('/paiements/config')
  return data
}

export async function preparerPaiement(demande) {
  const { data } = await api.post('/paiements/intent', demande)
  return data
}

export async function payerEnSimulation(intention, numeroCarte) {
  await api.post(`/paiements/simulation/${intention.paymentIntentId}/payer`, {
    clientSecret: intention.clientSecret,
    numeroCarte,
  })
}

const LOCALES = { fr: 'fr-BE', nl: 'nl-BE', en: 'en-GB' }
const locale = (langue) => LOCALES[langue] ?? LOCALES.fr

// L'API exprime les créneaux à l'heure de l'agence, sans fuseau : « 2026-10-08T18:30:00 »
export const jourDe = (dateHeure) => dateHeure.slice(0, 10)

export const formatJour = (dateHeure, langue) =>
  new Intl.DateTimeFormat(locale(langue), { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date(dateHeure))

export const formatHeure = (dateHeure, langue) =>
  new Intl.DateTimeFormat(locale(langue), { hour: '2-digit', minute: '2-digit' }).format(new Date(dateHeure))

export const formatMontant = (montant, langue) =>
  new Intl.NumberFormat(locale(langue), { style: 'currency', currency: 'EUR' }).format(montant)
