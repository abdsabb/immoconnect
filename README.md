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
| Conteneurisation | Docker Compose (dev) · images Docker + Nginx (prod) |
| Intégration continue | GitHub Actions à chaque push : tests backend (Testcontainers), build frontend, images Docker |

## Structure du dépôt

```
├── backend/                 API Spring Boot (Maven) — Dockerfile multi-étapes
│   └── src/main/resources/db/migration/   V1 = schéma (17 tables), V2 = données de test
├── frontend/                SPA React (Vite) — Dockerfile + nginx.conf
├── api/openapi.yaml         Spécification OpenAPI 3.0 de l'API (livrable 15)
├── docs/uml/                Sources PlantUML des diagrammes d'analyse (livrable 07) et du schéma BDD
├── .github/workflows/ci.yml Intégration continue
└── docker-compose.yml       Environnement de développement (MySQL 8.4 + Adminer)
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
| Administrateur | david.moreau@mail.be |

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
| GET | `/api/v1/paiements/config` — mode de paiement, clé publiable, prix du créneau premium | public |
| POST | `/api/v1/paiements/intent` — préparer le paiement Stripe d'un créneau premium | JWT membre |
| POST | `/api/v1/webhooks/stripe` — notifications de paiement | signature Stripe |

Documentation interactive : `/swagger-ui.html` · erreurs au format problem+json (RFC 7807).

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
