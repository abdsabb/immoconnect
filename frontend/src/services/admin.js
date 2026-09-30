import { api } from './api'

// Back-office de l'administrateur (cas A1 à A8) et blog public (cas V5)

const donnees = (reponse) => reponse.data
const sansVide = (params) => Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v !== null && v !== undefined))

// A8 — statistiques
export const chargerStatistiques = () => api.get('/admin/statistiques').then(donnees)

// A1 — comptes
export const chargerComptes = (filtres) => api.get('/admin/utilisateurs', { params: sansVide(filtres) }).then(donnees)
export const creerAgent = (agent) => api.post('/admin/agents', agent).then(donnees)
export const changerActivation = (id, actif) => api.patch(`/admin/utilisateurs/${id}/${actif ? 'activer' : 'desactiver'}`).then(donnees)

// A4 — journal d'audit
export const chargerJournal = (filtres) => api.get('/admin/journal', { params: sansVide(filtres) }).then(donnees)
export const chargerActionsJournal = () => api.get('/admin/journal/actions').then(donnees)

// A3 — catégories de biens
export const creerCategorie = (categorie) => api.post('/admin/categories', categorie).then(donnees)
export const modifierCategorie = (id, categorie) => api.put(`/admin/categories/${id}`, categorie).then(donnees)
export const supprimerCategorie = (id) => api.delete(`/admin/categories/${id}`)

// A7 — clés API
export const chargerClesApi = () => api.get('/admin/cles-api').then(donnees)

// Signalements de contenus (chapitre 11) et paramètres du site (A5)
export const chargerSignalements = (filtres) => api.get('/admin/signalements', { params: sansVide(filtres) }).then(donnees)
export const trancherSignalement = (id, decision) => api.patch(`/admin/signalements/${id}`, decision).then(donnees)
export const chargerParametres = () => api.get('/admin/parametres').then(donnees)
export const chargerAlertes = (filtres) => api.get('/admin/alertes', { params: sansVide(filtres) }).then(donnees)
export const enregistrerParametres = (valeurs) => api.put('/admin/parametres', { valeurs }).then(donnees)
export const genererCleApi = (libelle) => api.post('/admin/cles-api', { libelle }).then(donnees)
export const revoquerCleApi = (id) => api.patch(`/admin/cles-api/${id}/revoquer`).then(donnees)

// A6 — traductions
export const chargerTraductions = () => api.get('/admin/traductions').then(donnees)
export const enregistrerTraduction = (cle, valeurs) => api.put(`/admin/traductions/${encodeURIComponent(cle)}`, { valeurs }).then(donnees)
export const supprimerTraduction = (cle) => api.delete(`/admin/traductions/${encodeURIComponent(cle)}`)

// A2 — articles du blog
export const chargerArticlesAdmin = (filtres) => api.get('/admin/articles', { params: sansVide(filtres) }).then(donnees)
export const chargerArticleAdmin = (id) => api.get(`/admin/articles/${id}`).then(donnees)
export const creerArticle = (article) => api.post('/admin/articles', article).then(donnees)
export const modifierArticle = (id, article) => api.put(`/admin/articles/${id}`, article).then(donnees)
export const changerStatutArticle = (id, action) => api.patch(`/admin/articles/${id}/${action}`).then(donnees)
export const supprimerArticle = (id) => api.delete(`/admin/articles/${id}`)

// V5 — blog public
export const chargerArticles = (filtres) => api.get('/articles', { params: sansVide(filtres) }).then(donnees)
export const chargerArticle = (id) => api.get(`/articles/${id}`).then(donnees)
export const chargerCategoriesBlog = () => api.get('/articles/categories').then(donnees)

const LOCALES = { fr: 'fr-BE', nl: 'nl-BE', en: 'en-GB' }
export const formatDate = (date, langue) =>
  date ? new Intl.DateTimeFormat(LOCALES[langue] ?? LOCALES.fr, { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(date)) : '—'
export const formatDateHeure = (date, langue) =>
  date ? new Intl.DateTimeFormat(LOCALES[langue] ?? LOCALES.fr, { dateStyle: 'short', timeStyle: 'short' }).format(new Date(date)) : '—'
export const formatEuros = (montant, langue) =>
  new Intl.NumberFormat(LOCALES[langue] ?? LOCALES.fr, { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(montant)
