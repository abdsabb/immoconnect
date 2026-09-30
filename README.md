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
| Front-end | React 19 + Vite · Tailwind CSS 4 · React Router · react-i18next · TanStack Query · react-leaflet · FullCalendar · Vitest + React Testing Library |
| Back-end | Spring Boot 4.1 (Java 21 LTS) — API REST `/api/v1` · Spring Security · Spring Data JPA · springdoc (Swagger) |
| Base de données | MySQL 8.4 LTS — migrations Flyway (`backend/src/main/resources/db/migration`) |
| Paiement | Stripe (PaymentIntents + webhooks signés) |
| Conteneurisation | Docker Compose (dev et prod) · images Docker, Nginx et Caddy (HTTPS) en production |
| Intégration continue | GitHub Actions à chaque push : tests backend (Testcontainers), lint, tests et build frontend (Vitest), images Docker |

## Structure du dépôt

```
├── backend/                 API Spring Boot (Maven) — Dockerfile multi-étapes
│   └── src/main/resources/db/migration/   V1 = schéma (17 tables), V2 = données de test,
│                                          V3 = expéditeur des messages, V4 = back-office administrateur,
│                                          V5 = légendes des photos de test, V6 = biens à vendre ou à louer
│   └── src/main/resources/photos-demo/    Photos des annonces de test (licences libres, voir CREDITS.md)
├── frontend/                SPA React (Vite) — Dockerfile + nginx.conf
├── api/openapi.yaml         Spécification OpenAPI 3.0 de l'API (livrable 15)
├── docs/uml/                Sources PlantUML des diagrammes d'analyse (livrable 07) et du schéma BDD
├── .github/workflows/ci.yml Intégration continue
├── docker-compose.yml       Environnement de développement (MySQL 8.4 + Adminer + Mailpit)
├── docker-compose.prod.yml  Production : Caddy (HTTPS), frontend, backend, MySQL, Mailpit
├── deploiement/Caddyfile    Routage et certificat HTTPS de la production
└── .env.example             Modèle du fichier .env de production
```

## Organisation du back-end

Le code Java (`backend/src/main/java/be/immoconnect`) est rangé par couche :

| Paquet | Rôle |
|---|---|
| `entities` | Entités JPA et énumérations : les objets de la base de données |
| `repositories` | Dépôts Spring Data JPA : l'accès à la base de données |
| `services` | Logique métier et règles d'application (RA1 à RA13) |
| `controllers` | Contrôleurs REST : les points d'entrée de l'API |
| `dto` | Objets d'échange de l'API — une entité n'est jamais renvoyée telle quelle |
| `security` | Spring Security, jetons JWT, clés API et quota |
| `exceptions` | Exceptions métier et leur conversion en réponses problem+json |
| `config` | Horloge et fuseau de l'agence, documentation OpenAPI |
| `paiement` · `notification` · `stockage` | Adaptateurs vers l'extérieur : Stripe, e-mail, disque |

Une requête traverse les couches dans un seul sens : contrôleur, service, dépôt. Un contrôleur ne touche jamais
un dépôt, et un service ne connaît ni HTTP ni JSON. Les tests suivent la même organisation.

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
Tests frontend (Vitest + React Testing Library : formulaires, validations, étape du code) : `cd frontend && npm test`.

Le code du front-end est découpé par route (`React.lazy`) : les pages publiques les plus visitées partent avec
l'application, les espaces connectés, le blog, les pages légales et la carte Leaflet se chargent à la première visite.

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

Les agents et les administrateurs se connectent en deux étapes : après le mot de passe, un code à six chiffres est
envoyé par e-mail. En développement, il se lit dans Mailpit (http://localhost:8025) ; en production, dans la boîte
de démonstration `/courriels/`. Les comptes créés depuis le site reçoivent un lien d'activation au même endroit.

Les photos téléversées sont enregistrées dans `backend/stockage/` (variable `STORAGE_DIR`), hors du dépôt.

### Sécurité des comptes

- **Sessions** : le jeton d'accès (JWT HS256) vit 15 minutes ; un cookie de session `immoconnect_session` (HttpOnly,
  Secure, SameSite=Strict, limité à `/api/v1/auth`) le renouvelle pendant 14 jours. Le cookie est tourné à chaque
  renouvellement ; la réutilisation d'un ancien cookie ferme toutes les sessions du compte. Les jetons ne sont
  stockés qu'en empreinte SHA-256 (table `jeton`).
- **Mots de passe** : 8 à 72 caractères, minuscules, majuscules et un chiffre, refusés s'ils figurent dans une fuite
  connue (Have I Been Pwned, par k-anonymat : cinq caractères de l'empreinte SHA-1 partent, jamais le mot de passe ;
  repli sur une liste embarquée si le service est injoignable). Hachage bcrypt.
- **Double facteur** : code à six chiffres par e-mail, valable 10 minutes, cinq essais ; imposé aux agents et aux
  administrateurs, au choix pour les membres (profil).
- **Blocage** : après 5 échecs, le compte est verrouillé 1 minute, puis le double à chaque série, jusqu'à 15 minutes ;
  une adresse inconnue est bloquée de la même façon, sans révéler qu'elle est inconnue ; 10 connexions par minute et
  par adresse IP, au-delà `429` avec `Retry-After`.
- **Activation et réinitialisation** : lien d'activation valable 24 heures, lien de réinitialisation 30 minutes, à
  usage unique ; la réinitialisation ferme toutes les sessions et prévient par e-mail.
- **Inscription** : acceptation des conditions générales obligatoire et horodatée (`cgu_acceptees_le`), consentement
  aux communications distinct.
- **Droits RGPD** : accès et rectification (profil), portabilité (`GET /auth/me/export`, fichier JSON), effacement
  (désinscription RA11). **Signalement de contenus** (DSA) : lien « Signaler ce contenu » sous chaque message reçu,
  annonce et article ; un gestionnaire retire (message vidé, annonce hors ligne, article archivé) ou conserve, avec
  une décision motivée, définitive et journalisée.

Ces mécanismes se règlent dans `application.yml` (`immoconnect.securite.*`) et par les variables
`ACTIVATION_PAR_COURRIEL`, `DOUBLE_FACTEUR`, `MOTS_DE_PASSE_COMPROMIS` et `COOKIE_SECURE` (voir `.env.example`).

### Biens à vendre et biens à louer

Chaque annonce porte un type d'offre, `vente` ou `location`, qui donne son sens au prix : prix de vente, ou loyer
mensuel. Le site a une page par type (`/a-vendre`, `/a-louer`) et l'API filtre par `typeOffre`. Deux règles sont
appliquées par le service et par une contrainte de la base : un bien à vendre ne devient pas « loué », un bien à
louer ne devient pas « vendu ». Les statistiques ne mélangent jamais un prix de vente et un loyer.

### Flux RSS

Deux flux RSS 2.0 publics annoncent les nouveautés du site : les 20 derniers articles du blog et les 20 dernières
annonces disponibles, à vendre, à louer ou les deux. Un flux ne montre que ce qu'un visiteur voit déjà : ni brouillon,
ni bien retiré, ni adresse exacte, ni nom d'agent. Le document est produit par l'écrivain XML du JDK, qui échappe
les textes. Les pages du site déclarent les flux dans leur en-tête, et les affichent par un lien « Flux RSS ».

### Référencement : pages publiques rendues côté serveur

Une application React renvoie par défaut une page vide que le navigateur remplit ensuite. Pour les moteurs de
recherche, les pages publiques — accueil, `/a-vendre`, `/a-louer`, `/biens`, `/biens/{id}`, `/blog`, `/blog/{id}` —
sont donc **rendues par le serveur** : Nginx les confie au backend (`/rendu/...`), qui complète l'`index.html` construit
avec le titre, la description, la balise canonique, les balises `hreflang` (fr, nl, en, selon les langues actives), les
données **Schema.org** (`RealEstateAgent`, `RealEstateListing`, `ItemList`, `Blog`, `BlogPosting`) et le contenu
visible. Le navigateur affiche cette page tout de suite, puis l'application React prend le relais. Un bien archivé ou
un article non publié répond `404`. Si le backend ne répond pas, Nginx sert l'application seule.

- `?lng=nl` choisit la langue d'une page : c'est l'adresse des versions linguistiques annoncées par `hreflang`.
- `/sitemap.xml` : plan du site généré depuis les données (biens disponibles, articles publiés, trois langues).
- `/robots.txt` : pages publiques indexables ; espaces connectés, API et boîte de démonstration exclus.
- En développement (`npm run dev`), Vite sert l'application sans rendu serveur ; `RENDU_COQUILLE` l'active.

### Photos des annonces de test

Les annonces de test référencent 498 photos. Au démarrage, le backend crée celles qui manquent dans le dossier
de stockage, à partir de 112 photos de [Wikimedia Commons](https://commons.wikimedia.org) embarquées dans
l'application ; la photo est choisie d'après la légende (façade, séjour, cuisine…). Un fichier déjà présent
n'est jamais remplacé, et une photo téléversée par un agent n'est pas concernée. `DEMO_PHOTOS=false` désactive
cette étape.

Auteurs et licences : [CREDITS.md](backend/src/main/resources/photos-demo/CREDITS.md), repris sur le site à la
page `/credits-photos`.

### API disponible

| Méthode | Endpoint | Accès |
|---|---|---|
| GET | `/api/v1/biens` — recherche multicritères paginée (type d'offre, ville, catégorie, prix, chambres, superficie, tri) | public |
| GET | `/api/v1/biens/{id}` — détail, photos, agent (adresse exacte masquée) | public |
| GET | `/api/v1/traductions/{fr\|nl\|en}` — dictionnaire d'interface | public |
| POST | `/api/v1/auth/register` · `/api/v1/auth/login` — inscription (202 si activation par e-mail), connexion (202 et défi si second facteur) | public |
| POST | `/api/v1/auth/login/code` · `/auth/refresh` · `/auth/logout` — code du second facteur, renouvellement par le cookie de session, déconnexion | public |
| POST | `/api/v1/auth/mot-de-passe-oublie` · `/auth/reinitialisation` · `/auth/activation` · `/auth/activation/renvoi` — liens à usage unique reçus par e-mail | public |
| GET | `/api/v1/configuration` — options publiques : activation, double facteur, boîte de démonstration, identité de l'agence et langues actives (A5) | public |
| GET | `/api/v1/auth/me/export` — toutes mes données en JSON (portabilité, RGPD) | JWT |
| POST | `/api/v1/signalements` — signaler un message reçu, une annonce ou un article (DSA) | JWT |
| GET / PATCH / DELETE | `/api/v1/auth/me` — profil, modification, désinscription (soft delete RA11) | JWT |
| PUT | `/api/v1/auth/me/mot-de-passe` | JWT |
| GET | `/api/v1/biens/{id}/creneaux` — créneaux de visite libres, standard et premium | JWT membre |
| GET | `/api/v1/rendez-vous` — mes visites (membre) ou mon agenda (agent) | JWT |
| POST | `/api/v1/rendez-vous` — réserver un créneau (409 si le créneau vient d'être pris) | JWT membre |
| PATCH | `/api/v1/rendez-vous/{id}/confirmer` · `/honorer` | JWT agent du rendez-vous |
| PATCH | `/api/v1/rendez-vous/{id}/annuler` — rembourse un créneau premium payé (RA8), sauf annulation par le membre moins de 24 h avant la visite (RA14) | JWT membre ou agent du rendez-vous |
| PUT / DELETE | `/api/v1/biens/{id}/favori` — ajouter ou retirer un favori (idempotent) | JWT membre |
| GET | `/api/v1/membres/moi/favoris` — mes favoris, paginés | JWT membre |
| GET / POST | `/api/v1/messages` — mes conversations, envoyer un message | JWT membre ou agent |
| GET | `/api/v1/messages/conversations/{interlocuteurId}` — messages échangés avec un interlocuteur | JWT membre ou agent |
| PATCH | `/api/v1/messages/conversations/{interlocuteurId}/lu` — marquer les messages reçus comme lus | JWT membre ou agent |
| GET | `/api/v1/paiements/config` — mode de paiement, clé publiable, prix du créneau premium | public |
| POST | `/api/v1/paiements/intent` — préparer le paiement Stripe d'un créneau premium (carte ou Bancontact, `STRIPE_MOYENS`) | JWT membre |
| POST | `/api/v1/webhooks/stripe` — notifications de paiement | signature Stripe |
| GET | `/api/v1/categories` — catégories de biens | public |
| GET | `/api/v1/articles` · `/articles/{id}` · `/articles/categories` — blog, articles publiés uniquement (RA4) | public |
| GET | `/api/v1/agents/moi/biens` — mes annonces et leur tableau de bord | JWT agent |
| POST / PUT / DELETE | `/api/v1/biens` · `/biens/{id}` — créer, modifier, archiver une annonce (RA6) | JWT agent responsable |
| POST / DELETE / PUT | `/api/v1/biens/{id}/photos` · `/photos/{photoId}` · `/photos/{photoId}/couverture` | JWT agent responsable |
| GET / POST / PATCH | `/api/v1/admin/utilisateurs` · `/admin/agents` · `/utilisateurs/{id}/activer` · `/desactiver` | JWT admin, niveau 2 |
| GET | `/api/v1/admin/journal` · `/admin/statistiques` — journal d'audit filtrable, statistiques | JWT admin, niveau 2 |
| GET / POST / PATCH | `/api/v1/admin/cles-api` · `/cles-api/{id}/revoquer` — clés API (RA12) | JWT admin, niveau 2 |
| GET / PATCH | `/api/v1/admin/signalements` · `/signalements/{id}` — signalements à trancher : retirer ou conserver, décision motivée | JWT admin, niveau 2 |
| GET / PUT | `/api/v1/admin/parametres` — nom, coordonnées, horaires de l'agence, langues actives (A5, A6) | JWT admin, niveau 2 |
| POST / PUT / DELETE | `/api/v1/admin/categories` · `/admin/articles` · `/admin/traductions/{cle}` | JWT admin, niveau 1 |
| GET | `/api/v1/flux/articles` · `/flux/biens?typeOffre=` — flux RSS 2.0 des derniers articles et des dernières annonces | public |
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
