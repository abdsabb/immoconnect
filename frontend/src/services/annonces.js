import { api } from './api'

// Back-office de l'agent (cas AG1, AG2, AG3, AG6) : ses annonces et leurs photos

export async function chargerCategories() {
  const { data } = await api.get('/categories')
  return data
}

export async function chargerMesAnnonces() {
  const { data } = await api.get('/agents/moi/biens')
  return data
}

export async function chargerAnnonce(id) {
  const { data } = await api.get(`/agents/moi/biens/${id}`)
  return data
}

export async function creerAnnonce(annonce) {
  const { data } = await api.post('/biens', annonce)
  return data
}

export async function modifierAnnonce(id, annonce) {
  const { data } = await api.put(`/biens/${id}`, annonce)
  return data
}

export async function archiverAnnonce(id) {
  await api.delete(`/biens/${id}`)
}

export async function ajouterPhoto(id, fichier, legende) {
  const formulaire = new FormData()
  formulaire.append('fichier', fichier)
  if (legende) formulaire.append('legende', legende)
  const { data } = await api.post(`/biens/${id}/photos`, formulaire)
  return data
}

export async function supprimerPhoto(id, photoId) {
  const { data } = await api.delete(`/biens/${id}/photos/${photoId}`)
  return data
}

export async function definirCouverture(id, photoId) {
  const { data } = await api.put(`/biens/${id}/photos/${photoId}/couverture`)
  return data
}

/** Les champs numériques d'un formulaire HTML sont des chaînes : l'API attend des nombres. */
export function versRequete(valeurs) {
  return {
    categorieId: Number(valeurs.categorieId),
    titre: valeurs.titre,
    description: valeurs.description,
    prix: Number(valeurs.prix),
    superficie: Number(valeurs.superficie),
    nbChambres: Number(valeurs.nbChambres),
    adresse: valeurs.adresse,
    ville: valeurs.ville,
    codePostal: valeurs.codePostal,
    latitude: Number(valeurs.latitude),
    longitude: Number(valeurs.longitude),
    statut: valeurs.statut || null,
  }
}

// Statuts qu'un agent peut donner à une annonce ; « archive » s'affiche « hors ligne » dans le back-office
export const STATUTS = ['disponible', 'sous_option', 'vendu', 'loue', 'archive']
export const TAILLE_MAX_PHOTO = 5 * 1024 * 1024
