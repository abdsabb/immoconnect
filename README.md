# ImmoConnect

Plateforme web d'agence immobilière — Épreuve intégrée, Bachelier en Informatique, orientation développement d'applications (ICC Bruxelles, 2025-2026).

**Auteur :** Abdulrahman Sabbagh

## Le projet

ImmoConnect permet à une agence immobilière bruxelloise de publier ses biens, de mettre en relation
visiteurs et agents, et de digitaliser la **prise de rendez-vous de visite** — avec des créneaux
standard gratuits et des créneaux **premium payants** (soirée/week-end, 15 €, paiement Stripe).
Le site est multilingue (FR/NL/EN), cartographié avec OpenStreetMap, et expose une API REST
documentée ainsi qu'un volet Open Data.

## Pile technique

| Couche | Technologie |
|---|---|
| Front-end | React 19 + Vite · Tailwind CSS 4 · React Router · react-i18next · TanStack Query · react-leaflet |
| Back-end | Spring Boot 4.1 (Java 21 LTS) — API REST `/api/v1` · Spring Security · Spring Data JPA · springdoc (Swagger) |
| Base de données | MySQL 8.4 LTS — migrations Flyway (`backend/src/main/resources/db/migration`) |
| Paiement | Stripe (PaymentIntents + webhooks signés) |
| Conteneurisation | Docker Compose (dev et prod) · images Docker, Nginx et Caddy (HTTPS) en production |
| Intégration continue | GitHub Actions à chaque push : tests backend (Testcontainers), build frontend, images Docker |

## Structure du dépôt

```
├── backend/                 API Spring Boot (Maven) — Dockerfile multi-étapes
│   └── src/main/resources/db/migration/   V1 = schéma (17 tables), V2 = données de test,
│                                          V3 = expéditeur des messages, V4 = back-office administrateur
├── frontend/                SPA React (Vite) — Dockerfile + nginx.conf
├── api/openapi.yaml         Spécification OpenAPI 3.0 de l'API (livrable 15)
├── docs/uml/                Sources PlantUML des diagrammes d'analyse (livrable 07) et du schéma BDD
├── .github/workflows/ci.yml Intégration continue
├── docker-compose.yml       Environnement de développement (MySQL 8.4 + Adminer + Mailpit)
├── docker-compose.prod.yml  Production : Caddy (HTTPS), frontend, backend, MySQL, Mailpit
├── deploiement/Caddyfile    Routage et certificat HTTPS de la production
└── .env.example             Modèle du fichier .env de production
```

## Démarrer en développement

```bash
# 1. Base de données MySQL 8.4 (+ Adminer sur http://localhost:8081 — serveur: db, user: immo, mdp: immo)
#    et boîte de réception Mailpit, qui capture les e-mails de l'application (http://localhost:8025)
docker compose up -d db adminer mailpit

# 2. Backend — profil dev par défaut ; Flyway crée le schéma et charge les données de test au premier démarrage
cd backend && ./mvnw spring-boot:run
#    API : http://localhost:8080/api/v1  ·  Swagger UI : http://localhost:8080/swagger-ui.html

# 3. Frontend — serveur Vite avec proxy /api vers le backend
cd frontend && npm install && npm run dev
#    http://localhost:5173
```

Tests backend (nécessitent Docker, un MySQL 8.4 jetable est lancé par Testcontainers) : `cd backend && ./mvnw verify`.

> Dépannage sans Docker : le profil par défaut fonctionne aussi avec le MySQL/MariaDB de XAMPP
> (base `immoconnect`, utilisateur `immo` / `immo`). La référence reste MySQL 8.4.

### Paiements : Stripe ou mode simulation

Sans clé Stripe, le backend démarre en **mode simulation** : le parcours de paiement est complet, avec
les cartes de test `4242 4242 4242 4242` (acceptée) et `4000 0000 0000 0002` (refusée), sans aucun appel réseau.
C'est aussi le mode utilisé par les tests automatisés.

Pour utiliser Stripe en mode test, fournir les clés par variables d'environnement avant de lancer le backend
(elles ne sont jamais versionnées ; en production, leur absence arrête le démarrage) :

```bash
export STRIPE_SECRET_KEY=sk_test_...        # PowerShell : $env:STRIPE_SECRET_KEY = "sk_test_..."
export STRIPE_PUBLISHABLE_KEY=pk_test_...
export STRIPE_WEBHOOK_SECRET=whsec_...       # affiché par : stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
```

### Comptes de test (mot de passe : `password` pour tous, hachés bcrypt en base)

| Rôle | E-mail |
|---|---|
| Membre | alice.benali@mail.be |
| Agent immobilier | sarah.dubois@mail.be |
| Administrateur — super-administrateur (niveau 3) | david.moreau@mail.be |
| Administrateur — gestionnaire (niveau 2) | lotte.goossens@mail.be |
| Administrateur — éditeur (niveau 1) | yasmine.benali@mail.be |

Le niveau d'accès d'un administrateur limite ce qu'il peut faire dans le back-office : l'éditeur gère le blog,
les catégories et les traductions ; le gestionnaire gère en plus les comptes, le journal d'audit, les statistiques
et les clés API ; le super-administrateur agit aussi sur les comptes des administrateurs.

Les photos téléversées sont enregistrées dans `backend/stockage/` (variable `STORAGE_DIR`), hors du dépôt.

### API disponible

| Méthode | Endpoint | Accès |
|---|---|---|
| GET | `/api/v1/biens` — recherche multicritères paginée (ville, catégorie, prix, chambres, superficie, tri) | public |
| GET | `/api/v1/biens/{id}` — détail, photos, agent (adresse exacte masquée) | public |
| GET | `/api/v1/traductions/{fr\|nl\|en}` — dictionnaire d'interface | public |
| POST | `/api/v1/auth/register` · `/api/v1/auth/login` — inscription, jeton JWT | public |
| GET / PATCH / DELETE | `/api/v1/auth/me` — profil, modification, désinscription (soft delete RA11) | JWT |
| PUT | `/api/v1/auth/me/mot-de-passe` | JWT |
| GET | `/api/v1/biens/{id}/creneaux` — créneaux de visite libres, standard et premium | JWT membre |
| GET | `/api/v1/rendez-vous` — mes visites (membre) ou mon agenda (agent) | JWT |
| POST | `/api/v1/rendez-vous` — réserver un créneau (409 si le créneau vient d'être pris) | JWT membre |
| PATCH | `/api/v1/rendez-vous/{id}/confirmer` · `/honorer` | JWT agent du rendez-vous |
| PATCH | `/api/v1/rendez-vous/{id}/annuler` — rembourse un créneau premium payé (RA8) | JWT membre ou agent du rendez-vous |
| PUT / DELETE | `/api/v1/biens/{id}/favori` — ajouter ou retirer un favori (idempotent) | JWT membre |
| GET | `/api/v1/membres/moi/favoris` — mes favoris, paginés | JWT membre |
| GET / POST | `/api/v1/messages` — mes conversations, envoyer un message | JWT membre ou agent |
| GET | `/api/v1/messages/conversations/{interlocuteurId}` — messages échangés avec un interlocuteur | JWT membre ou agent |
| PATCH | `/api/v1/messages/conversations/{interlocuteurId}/lu` — marquer les messages reçus comme lus | JWT membre ou agent |
| GET | `/api/v1/paiements/config` — mode de paiement, clé publiable, prix du créneau premium | public |
| POST | `/api/v1/paiements/intent` — préparer le paiement Stripe d'un créneau premium | JWT membre |
| POST | `/api/v1/webhooks/stripe` — notifications de paiement | signature Stripe |
| GET | `/api/v1/categories` — catégories de biens | public |
| GET | `/api/v1/articles` · `/articles/{id}` · `/articles/categories` — blog, articles publiés uniquement (RA4) | public |
| GET | `/api/v1/agents/moi/biens` — mes annonces et leur tableau de bord | JWT agent |
| POST / PUT / DELETE | `/api/v1/biens` · `/biens/{id}` — créer, modifier, archiver une annonce (RA6) | JWT agent responsable |
| POST / DELETE / PUT | `/api/v1/biens/{id}/photos` · `/photos/{photoId}` · `/photos/{photoId}/couverture` | JWT agent responsable |
| GET / POST / PATCH | `/api/v1/admin/utilisateurs` · `/admin/agents` · `/utilisateurs/{id}/activer` · `/desactiver` | JWT admin, niveau 2 |
| GET | `/api/v1/admin/journal` · `/admin/statistiques` — journal d'audit filtrable, statistiques | JWT admin, niveau 2 |
| GET / POST / PATCH | `/api/v1/admin/cles-api` · `/cles-api/{id}/revoquer` — clés API (RA12) | JWT admin, niveau 2 |
| POST / PUT / DELETE | `/api/v1/admin/categories` · `/admin/articles` · `/admin/traductions/{cle}` | JWT admin, niveau 1 |
| GET | `/api/v1/open-data/biens` · `/open-data/statistiques` — données anonymisées, CC BY 4.0, 60 appels/min | clé API (`X-API-Key`) |

Documentation interactive : `/swagger-ui.html` · erreurs au format problem+json (RFC 7807).

## Déployer en production

La production tourne sur un VPS Linux avec Docker. Le fichier `docker-compose.prod.yml` démarre cinq conteneurs ;
seul Caddy est exposé à Internet (ports 80 et 443), il obtient et renouvelle le certificat HTTPS.

| Conteneur | Rôle |
|---|---|
| `caddy` | Point d'entrée HTTPS : `/api`, `/storage` et Swagger vers le backend, le reste vers le frontend |
| `frontend` | Application React servie par Nginx |
| `backend` | API Spring Boot, profil `prod`, utilisateur non root |
| `db` | MySQL 8.4, sans port publié ; schéma et données de test appliqués par Flyway |
| `mailpit` | Boîte de réception de démonstration sur `/courriels/` : capture les e-mails, n'en envoie aucun |

```bash
git clone https://github.com/abdsabb/immoconnect.git && cd immoconnect
cp .env.example .env && nano .env        # domaine, mots de passe, clé JWT, clés Stripe
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps
```

Mettre à jour : `git pull` puis la même commande `up -d --build`. Les données survivent dans les volumes Docker
(`mysql-data`, `photos`). Le webhook Stripe se déclare dans le tableau de bord Stripe sur
`https://<domaine>/api/v1/webhooks/stripe` ; son secret va dans `STRIPE_WEBHOOK_SECRET`.

## Branches, commits et releases

- **`main`** : version stable — contient toujours la dernière version validée, fusion depuis `dev` par pull request
- **`dev`** : développement en cours
- Messages de commit conventionnels : `feat:`, `fix:`, `test:`, `docs:`, `ci:`, `chore:` — un commit = un changement cohérent
- Releases : `v0.1.0-alpha` (MVP : authentification, profil, navigation, design, catalogue) · `v0.2.0-beta` (intégrations : Stripe, rendez-vous, messagerie) · `v1.0.0` (version déployée présentée à la défense)

## Livrables associés (Chamilo)

07 Analyse UML (V1-V3) · 08 Schéma BDD · 09 Dictionnaire de données · 10 Charte graphique ·
11 Prototype navigable · 12 Rapport écrit · 13 Solutions techniques · 14 Dump SQL ·
15 Documentation API & Open Data · 16 Stratégie de sécurité · 17 SEO/SEA · 18 Cadre légal

© ImmoConnect — projet académique, tous droits réservés.
