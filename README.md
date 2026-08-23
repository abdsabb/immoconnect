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
docker compose up -d db adminer

# 2. Backend — profil dev par défaut ; Flyway crée le schéma et charge les données de test au premier démarrage
cd backend && ./mvnw spring-boot:run
#    API : http://localhost:8080/api/v1  ·  Swagger UI : http://localhost:8080/swagger-ui.html

# 3. Frontend — serveur Vite avec proxy /api vers le backend
cd frontend && npm install && npm run dev
#    http://localhost:5173
```

Tests backend (nécessitent Docker, un MySQL 8.4 jetable est lancé par Testcontainers) : `cd backend && ./mvnw verify`.

Comptes de test : tous les mots de passe sont `password` (hachés bcrypt en base).

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
