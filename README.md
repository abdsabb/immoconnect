# ImmoConnect

Plateforme web d'agence immobilière — Épreuve intégrée, Bachelier en Informatique de gestion (ICC Bruxelles, 2025-2026).

**Auteur :** Abdu [NOM]  ·  **Promoteur :** [Nom du professeur]

## Le projet

ImmoConnect permet à une agence immobilière bruxelloise de publier ses biens, de mettre en relation
visiteurs et agents, et de digitaliser la **prise de rendez-vous de visite** — avec des créneaux
standard gratuits et des créneaux **premium payants** (soirée/week-end, 15 €, paiement Stripe).
Le site est multilingue (FR/NL/EN), cartographié avec OpenStreetMap, et expose une API REST
documentée ainsi qu'un volet Open Data.

## Pile technique

| Couche | Technologie |
|---|---|
| Front-end | React 19 + Vite · Tailwind CSS · react-leaflet |
| Back-end | Spring Boot (Java 21 LTS) — API REST `/api/v1` |
| Base de données | MySQL 8.4 LTS — migrations Flyway (`db/migration`) |
| Paiement | Stripe (PaymentIntents + webhooks signés) |
| Conteneurisation | Docker Compose |

## Structure du dépôt

```
├── backend/        API Spring Boot (Maven)
├── frontend/       SPA React (Vite)
├── db/migration/   Migrations Flyway : V1 = schéma (17 tables), V2 = données de test
├── api/            Spécification OpenAPI 3.0 (Swagger)
├── docker-compose.yml
└── README.md
```

## Démarrer en local

```bash
docker compose up -d        # MySQL 8.4 initialisé avec le schéma + les données de test
# Adminer (client BDD web) : http://localhost:8081  —  serveur: db · user: immo · mdp: immo
```

Comptes de test : tous les mots de passe sont `password` (hachés bcrypt en base).

## Branches

- **`main`** : version stable — contient toujours la dernière version finale validée
- **`dev`** : développement en cours ; fusion vers `main` par pull request

## Livrables associés (Chamilo)

07 Analyse UML (V1-V3) · 08 Schéma BDD · 09 Dictionnaire de données · 10 Charte graphique ·
11 Prototype navigable · 12 Rapport écrit · 13 Solutions techniques · 14 Dump SQL ·
15 Documentation API & Open Data · 16 Stratégie de sécurité · 17 SEO/SEA · 18 Cadre légal

© ImmoConnect — projet académique, tous droits réservés.
