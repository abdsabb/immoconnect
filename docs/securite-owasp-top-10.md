# OWASP Top 10 dans ImmoConnect

L'OWASP est une fondation qui publie le classement des dix familles de failles les plus graves des applications
web. Ce document reprend l'édition **2021**, celle que cite le rapport (§ 9.4). Pour chaque risque, il dit ce que
c'est, donne un exemple d'attaque, puis montre où le code d'ImmoConnect s'en protège.

Tout ce qui est décrit ici existe dans le code : chaque protection renvoie au fichier qui la contient. Les limites
connues sont indiquées, pour ne rien affirmer au jury qui ne se vérifie pas.

## Vue d'ensemble

| Risque | En une phrase | Parade principale dans ImmoConnect |
|---|---|---|
| A01 Contrôle d'accès défaillant | Quelqu'un fait ce qu'il n'a pas le droit de faire | Rôles vérifiés par le serveur, puis contrôle « est-ce bien à toi ? » |
| A02 Défaillances cryptographiques | Des données sensibles sont lisibles ou mal protégées | HTTPS, BCrypt, jetons et clés stockés sous forme d'empreinte |
| A03 Injection | Une saisie est exécutée comme du code | Requêtes paramétrées, validation, échappement, CSP |
| A04 Conception non sécurisée | La règle elle-même est mal pensée | Règles métier et prix appliqués par le serveur, limites de débit |
| A05 Mauvaise configuration | Un réglage par défaut laisse une porte ouverte | Tout fermé par défaut, secrets hors du dépôt, en-têtes de sécurité |
| A06 Composants vulnérables | Une bibliothèque utilisée a une faille connue | Dependabot et intégration continue |
| A07 Défaillances d'authentification | On se fait passer pour un autre | Verrouillage, mots de passe compromis refusés, double facteur |
| A08 Défaillances d'intégrité | On fait confiance à des données ou du code non vérifiés | Signature des webhooks Stripe, images ré-encodées, Flyway, CI |
| A09 Journalisation insuffisante | Une attaque passe sans laisser de trace | Journal d'audit, alertes de sécurité, fail2ban |
| A10 Falsification de requête côté serveur (SSRF) | Le serveur va chercher une adresse choisie par l'attaquant | Aucune adresse fournie par l'utilisateur n'est appelée |

---

## A01 — Contrôle d'accès défaillant

**C'est quoi.** L'utilisateur est bien connecté, mais il accède à une ressource ou à une action qui ne lui
revient pas. C'est la faille la plus fréquente du classement.

**Exemple d'attaque.** Un agent connecté appelle `PUT /api/v1/biens/42` alors que le bien 42 appartient à un
autre agent. Ou un membre change le numéro dans l'adresse pour lire le rendez-vous d'un autre membre.

**Dans ImmoConnect.** Deux contrôles se suivent, tous deux côté serveur :

1. **Le rôle.** [ConfigurationSecurite.java](../backend/src/main/java/be/immoconnect/security/ConfigurationSecurite.java)
   liste les chemins publics ; tout le reste exige un jeton (`anyRequest().authenticated()`), et chaque famille de
   chemins exige un rôle (`hasRole("MEMBRE")`, `"AGENT"`, `"ADMIN"`). Rien n'est ouvert par oubli.
2. **La propriété.** Avoir le bon rôle ne suffit pas : le service vérifie que la ressource appartient à celui qui
   la demande, sinon il lève `OperationInterditeException` (réponse 403).
   - Annonces : [ServiceAnnonces.java](../backend/src/main/java/be/immoconnect/services/ServiceAnnonces.java)
     (« Cette annonce appartient à un autre agent »).
   - Rendez-vous : [ServiceRendezVous.java](../backend/src/main/java/be/immoconnect/services/ServiceRendezVous.java)
     (« Ce rendez-vous ne vous appartient pas »).
   - Back-office : [AccesAdministrateur.java](../backend/src/main/java/be/immoconnect/services/AccesAdministrateur.java)
     vérifie le niveau d'accès de l'administrateur (éditeur, gestionnaire, super-administrateur).

L'identité vient toujours du jeton (`jeton.getSubject()`), jamais d'un paramètre envoyé par le navigateur.

Côté navigateur, [RouteProtegee.jsx](../frontend/src/auth/RouteProtegee.jsx) cache les pages selon le rôle. C'est
du confort : la vraie barrière est le serveur.

**À montrer.** Se connecter en agent, tenter de modifier l'annonce d'un autre agent : réponse 403.

---

## A02 — Défaillances cryptographiques

**C'est quoi.** Des données sensibles circulent ou sont stockées en clair, ou avec un procédé trop faible.

**Exemple d'attaque.** Un attaquant vole une copie de la base et y lit les mots de passe. Ou il écoute le Wi-Fi
d'un café et lit un mot de passe envoyé en HTTP.

**Dans ImmoConnect.**

- **En transit** : tout passe en HTTPS. [Caddyfile](../deploiement/Caddyfile) obtient le certificat et pose
  l'en-tête `Strict-Transport-Security` (HSTS), qui interdit au navigateur de revenir en HTTP.
- **Mots de passe** : hachés avec BCrypt, facteur de coût 12 (`encodeurMotDePasse()` dans
  [ConfigurationSecurite.java](../backend/src/main/java/be/immoconnect/security/ConfigurationSecurite.java)).
  Un hachage ne se « déchiffre » pas, et BCrypt est lent exprès pour freiner la force brute.
- **Jetons de session, d'activation et de réinitialisation** : la base ne garde que leur empreinte SHA-256
  ([ServiceJetonsUniques.java](../backend/src/main/java/be/immoconnect/services/ServiceJetonsUniques.java)).
  Une base volée ne livre aucun jeton utilisable.
- **Clés API** : même principe, empreinte SHA-256 seulement
  ([ServiceClesApi.java](../backend/src/main/java/be/immoconnect/services/ServiceClesApi.java)). La clé n'est
  montrée qu'une fois, à sa création.
- **Jetons JWT** : signés en HS256 avec un secret d'au moins 32 octets, refusé au démarrage s'il est plus court
  ([ConfigurationJwt.java](../backend/src/main/java/be/immoconnect/security/ConfigurationJwt.java)).
- **Cookie de session** : `HttpOnly`, `Secure`, `SameSite=Strict`
  ([AuthControleur.java](../backend/src/main/java/be/immoconnect/controllers/AuthControleur.java)).
- **Cartes bancaires** : elles ne passent jamais par le serveur. Les champs de paiement sont ceux de Stripe.
- **Sauvegardes** : chiffrées en AES-256 ([sauvegarder.sh](../deploiement/sauvegarde/sauvegarder.sh)).

**Limites connues.** Les sauvegardes ne sont chiffrées que si `SAUVEGARDE_PASSPHRASE` est renseignée dans le
`.env` du serveur. La copie des sauvegardes hors du serveur reste manuelle.

---

## A03 — Injection

**C'est quoi.** Une donnée saisie par l'utilisateur est interprétée comme du code : du SQL dans la base
(injection SQL), ou du JavaScript dans la page d'un autre visiteur (XSS).

**Exemple d'attaque.** Dans le champ de recherche : `' OR 1=1 --` pour lire toute une table. Dans un message ou
un article : `<script>…</script>` pour voler la session de celui qui le lira.

**Dans ImmoConnect.**

- **SQL** : aucune requête n'est construite en collant du texte. Tout passe par Spring Data JPA, avec des
  paramètres nommés (`:statut`, `:categorieId`), par exemple dans
  [ArticleRepository.java](../backend/src/main/java/be/immoconnect/repositories/ArticleRepository.java).
  Le code ne contient aucune requête SQL native.
- **Validation des entrées** : chaque corps de requête est validé (`@Valid`) avant d'atteindre le service ; une
  donnée refusée donne une réponse 422 qui nomme le champ.
- **XSS dans l'application React** : React échappe tout ce qu'il affiche, et le code n'utilise jamais
  `dangerouslySetInnerHTML`. Le texte d'un article est découpé en paragraphes, intertitres et listes par
  [ContenuArticle.jsx](../frontend/src/components/ContenuArticle.jsx), sans jamais interpréter une balise.
- **XSS dans les pages rendues par le serveur** : tout texte venu de la base passe par la fonction d'échappement
  `e()` de [ServiceRenduPublic.java](../backend/src/main/java/be/immoconnect/services/ServiceRenduPublic.java).
- **E-mails HTML** : même échappement dans
  [MiseEnPageCourriel.java](../backend/src/main/java/be/immoconnect/notification/MiseEnPageCourriel.java) ; ce
  qu'un visiteur tape dans le formulaire de contact n'y devient ni balise, ni lien.
- **Deuxième barrière, la CSP** : l'en-tête `Content-Security-Policy` du [Caddyfile](../deploiement/Caddyfile)
  n'autorise que les scripts du site et de Stripe. Un script injecté malgré tout ne s'exécuterait pas.
- **Fichiers** : le nom d'un fichier téléversé n'entre jamais dans un chemin ; le serveur génère un nom aléatoire
  ([StockageDisque.java](../backend/src/main/java/be/immoconnect/stockage/StockageDisque.java)), ce qui écarte la
  remontée de répertoire (`../../`).

**À montrer.** Les tests envoient `<script>alert(1)</script>` dans un article et dans le formulaire de contact,
et vérifient qu'il ressort comme du texte
([BlogEtTraductionsTest.java](../backend/src/test/java/be/immoconnect/controllers/BlogEtTraductionsTest.java),
[MiseEnPageCourrielTest.java](../backend/src/test/java/be/immoconnect/notification/MiseEnPageCourrielTest.java)).

**Limite connue.** La CSP autorise les styles en ligne (`style-src 'unsafe-inline'`), dont l'interface a besoin.
Les scripts en ligne, eux, restent interdits.

---

## A04 — Conception non sécurisée

**C'est quoi.** Le code fait exactement ce qui est prévu, mais ce qui est prévu est dangereux. Ce n'est pas un
bogue : c'est une règle manquante ou mal pensée.

**Exemple d'attaque.** Le navigateur envoie le prix à payer, et l'attaquant le remplace par 0,01 €. Ou un robot
essaie des milliers de mots de passe parce que rien ne limite le nombre d'essais.

**Dans ImmoConnect.**

- **Le prix est fixé par le serveur.** Le montant d'un créneau premium vient de la configuration
  ([GrilleCreneaux.java](../backend/src/main/java/be/immoconnect/services/GrilleCreneaux.java)), jamais du
  navigateur. À la réservation, le serveur vérifie que le paiement présenté correspond bien à ce créneau
  ([ServiceRendezVous.java](../backend/src/main/java/be/immoconnect/services/ServiceRendezVous.java), « ce paiement
  ne correspond pas à ce créneau »).
- **Les règles métier sont appliquées côté serveur**, puis une seconde fois par les contraintes de la base
  (`CHECK`, clés étrangères, unicité dans
  [V1__schema_initial.sql](../backend/src/main/resources/db/migration/V1__schema_initial.sql)).
- **Les changements d'état sont contrôlés** : un rendez-vous ne passe que par les transitions prévues ; une
  transition interdite donne une réponse 409.
- **Limites de débit** : 10 tentatives de connexion par minute et par adresse IP
  ([GardeConnexion.java](../backend/src/main/java/be/immoconnect/security/GardeConnexion.java)), 60 appels par
  minute pour une clé API ([LimiteDebit.java](../backend/src/main/java/be/immoconnect/security/LimiteDebit.java)),
  5 messages par heure et par adresse pour le formulaire de contact
  ([ServiceContact.java](../backend/src/main/java/be/immoconnect/services/ServiceContact.java)). Au-delà, la
  réponse est 429.
- **Ne pas renseigner l'attaquant** : « mot de passe oublié » répond la même chose que le compte existe ou non,
  et une adresse inconnue se verrouille comme une adresse connue
  ([GardeConnexion.java](../backend/src/main/java/be/immoconnect/security/GardeConnexion.java)).
- **Robots** : le formulaire de contact contient un champ piège invisible ; s'il est rempli, la demande est
  ignorée sans message d'erreur
  ([ServiceContact.java](../backend/src/main/java/be/immoconnect/services/ServiceContact.java)).

**Limite connue.** Les compteurs des limites de débit vivent en mémoire : ils repartent de zéro quand le serveur
redémarre.

---

## A05 — Mauvaise configuration de sécurité

**C'est quoi.** Le code est correct, mais un réglage laisse une porte ouverte : port exposé, mot de passe par
défaut, message d'erreur trop bavard, en-tête de sécurité absent.

**Exemple d'attaque.** La base de données écoute sur Internet avec le mot de passe par défaut. Ou une erreur
affiche la trace technique complète, qui révèle les versions et la structure du code.

**Dans ImmoConnect.**

- **Fermé par défaut** : toute nouvelle route exige un jeton tant qu'elle n'est pas déclarée publique.
- **Un seul point d'entrée** : dans [docker-compose.prod.yml](../docker-compose.prod.yml), seul Caddy publie des
  ports (80 et 443). La base, le backend et les autres services ne sont joignables que sur le réseau interne.
- **Pare-feu du serveur** : seuls les ports 22, 80 et 443 sont ouverts
  ([LISEZMOI.md](../deploiement/fail2ban/LISEZMOI.md)).
- **Secrets hors du dépôt** : mots de passe, secret JWT et clés Stripe viennent du fichier `.env` du serveur,
  ignoré par Git ; [.env.example](../.env.example) n'en donne que le modèle.
- **Erreurs sans détail technique** : [GestionnaireErreurs.java](../backend/src/main/java/be/immoconnect/exceptions/GestionnaireErreurs.java)
  renvoie des messages génériques, et `include-stacktrace: never` interdit toute trace technique dans une réponse
  ([application.yml](../backend/src/main/resources/application.yml)).
- **En-têtes de sécurité** posés par [Caddyfile](../deploiement/Caddyfile) : CSP, `X-Frame-Options: DENY` (le site
  ne s'affiche dans aucun cadre), `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `Permissions-Policy`, et
  l'en-tête `Server` retiré.
- **Supervision limitée** : en production, seul l'état de santé est exposé
  ([application-prod.yml](../backend/src/main/resources/application-prod.yml)).
- **Moindre privilège** : le conteneur du backend tourne avec un utilisateur non root
  ([Dockerfile](../backend/Dockerfile)).

**Limites connues.** Le conteneur Nginx du frontend tourne encore en root. La documentation Swagger est publique
en production : c'est voulu, elle décrit l'API, et les routes qu'elle décrit restent protégées.

---

## A06 — Composants vulnérables et obsolètes

**C'est quoi.** L'application embarque une bibliothèque, un moteur ou une image dont une faille est connue et
publiée.

**Exemple d'attaque.** Une faille est annoncée dans une bibliothèque Java ; des robots testent aussitôt tous les
sites qui l'utilisent encore.

**Dans ImmoConnect.**

- **Dependabot** ([dependabot.yml](../.github/dependabot.yml)) surveille Maven et npm chaque semaine, les images
  Docker et les actions GitHub chaque mois, et ouvre une pull request par mise à jour.
- **Intégration continue** ([ci.yml](../.github/workflows/ci.yml)) : chaque mise à jour proposée est compilée et
  testée (backend, frontend, images Docker) avant de pouvoir être fusionnée.
- **Versions récentes et suivies** : Java 21 et Node 22 (versions à support long), Spring Boot 4, React 19,
  MySQL 8.4.
- **Peu de dépendances** : le partage sur les réseaux sociaux se fait par de simples liens, sans script tiers.

**Limites connues.** Des pull requests Dependabot sont ouvertes et pas encore triées. L'image de Mailpit est
suivie par l'étiquette `latest`, donc sans version fixée.

---

## A07 — Identification et authentification défaillantes

**C'est quoi.** Tout ce qui permet de se faire passer pour quelqu'un d'autre : mots de passe faibles, essais
illimités, session volée ou qui ne se ferme jamais.

**Exemple d'attaque.** Un robot essaie, sur le site, des couples e-mail et mot de passe sortis d'une fuite d'un
autre site (bourrage d'identifiants).

**Dans ImmoConnect.**

- **Mots de passe robustes et jamais compromis** : à l'inscription et au changement, le mot de passe est comparé
  aux fuites connues ([MotsDePasseFuites.java](../backend/src/main/java/be/immoconnect/security/MotsDePasseFuites.java)).
  Seuls les cinq premiers caractères de son empreinte partent vers le service : le mot de passe lui-même ne quitte
  jamais le serveur.
- **Verrouillage progressif** : après 5 échecs, le compte est verrouillé une minute, durée doublée à chaque
  nouvel échec, jusqu'à 15 minutes
  ([GardeConnexion.java](../backend/src/main/java/be/immoconnect/security/GardeConnexion.java)).
- **Bannissement de l'adresse** : 10 échecs, ou 5 comptes différents essayés en 10 minutes depuis une même
  adresse, la bannissent de la connexion pendant 15 minutes
  ([DetectionIntrusion.java](../backend/src/main/java/be/immoconnect/security/DetectionIntrusion.java)).
- **Sessions courtes et révocables** : le jeton d'accès vit 15 minutes et reste en mémoire du navigateur. Le
  jeton de rafraîchissement est dans un cookie illisible par les scripts ; il est à usage unique et remplacé à
  chaque utilisation. La déconnexion le révoque.
- **Double facteur** : chaque compte peut activer un code envoyé par e-mail à la connexion.
- **Mot de passe oublié** : lien à usage unique, valable 30 minutes ; la réinitialisation ferme toutes les
  sessions et prévient le titulaire par e-mail.
- **Activation par e-mail** du compte, activable par la variable `ACTIVATION_PAR_COURRIEL`.

**À montrer.** [SecuriteDesComptesTest.java](../backend/src/test/java/be/immoconnect/controllers/SecuriteDesComptesTest.java)
couvre le verrouillage, le renouvellement du cookie, la déconnexion, le mot de passe oublié et le double facteur.

**Évolution depuis le rapport.** Le rapport annonce un double facteur obligatoire pour les agents et les
administrateurs. Il est aujourd'hui proposé à tous les comptes, et chacun choisit de l'activer.

---

## A08 — Manque d'intégrité des données et du logiciel

**C'est quoi.** L'application fait confiance à du code ou à des données sans vérifier d'où ils viennent ni s'ils
ont été modifiés en route.

**Exemple d'attaque.** Quelqu'un appelle directement l'adresse du webhook de paiement en se faisant passer pour
Stripe, pour faire croire qu'une visite a été payée.

**Dans ImmoConnect.**

- **Webhooks Stripe signés** : chaque notification est vérifiée avec le secret partagé (`constructEvent` dans
  [PasserelleStripe.java](../backend/src/main/java/be/immoconnect/paiement/PasserelleStripe.java)). Une signature
  invalide est refusée.
- **Images téléversées** : le type est lu dans le contenu du fichier, pas dans son nom, puis l'image est toujours
  ré-encodée en JPEG ([ImagesBiens.java](../backend/src/main/java/be/immoconnect/stockage/ImagesBiens.java)).
  Seuls les pixels survivent : un contenu caché dans le fichier disparaît, ainsi que la position GPS de la photo.
- **Base de données** : Flyway garde une somme de contrôle de chaque migration ; une migration modifiée après
  coup fait échouer le démarrage au lieu de s'appliquer en silence.
- **Chaîne de livraison** : rien n'entre dans `main` sans pull request et sans intégration continue verte.
- **Images Docker** officielles, avec une version fixée (`eclipse-temurin:21`, `node:22`, `nginx:1.27`,
  `mysql:8.4`).

**Limite connue.** Les images Docker sont fixées par numéro de version, pas par empreinte.

---

## A09 — Carences de journalisation et de surveillance

**C'est quoi.** Une attaque a lieu et personne ne le sait, parce que rien n'est enregistré ou que personne ne
regarde.

**Exemple d'attaque.** Un robot essaie des mots de passe pendant des semaines sans déclencher la moindre alerte.

**Dans ImmoConnect.**

- **Journal d'audit** : chaque action sensible (connexion, échec de connexion, changement de mot de passe,
  paiement, remboursement, création, modification, suppression, export de données) est enregistrée avec son auteur, l'adresse IP et l'heure
  ([ServiceAudit.java](../backend/src/main/java/be/immoconnect/services/ServiceAudit.java), table `journal_audit`).
  L'administrateur le consulte dans le back-office, rubrique « Journal d'audit ».
- **Alertes de sécurité** : la détection d'intrusion enregistre trois types d'alerte (rafale d'échecs,
  énumération d'identifiants, usage d'une clé API révoquée) dans la table `alerte_securite`, les affiche dans la
  rubrique « Sécurité » et prévient les super-administrateurs par e-mail
  ([DetectionIntrusion.java](../backend/src/main/java/be/immoconnect/security/DetectionIntrusion.java)).
- **Niveau serveur** : fail2ban lit ces alertes dans le journal du backend et ferme les ports web à l'adresse
  pendant une heure, une journée en cas de récidive ([LISEZMOI.md](../deploiement/fail2ban/LISEZMOI.md)).
- **Le journal lui-même ne fuit rien** : une ligne ne contient que l'auteur, l'action, la ressource, l'adresse IP
  et l'heure. Ni mot de passe, ni jeton.

**À montrer.** Trois échecs de connexion volontaires, puis la ligne correspondante dans le journal d'audit et,
au-delà du seuil, l'alerte dans la rubrique « Sécurité ».

**Limite connue.** L'alerte part par e-mail ; il n'y a pas de service de surveillance externe.

---

## A10 — Falsification de requête côté serveur (SSRF)

**C'est quoi.** L'attaquant donne une adresse au serveur, et le serveur va la chercher pour lui. Il peut ainsi
atteindre des machines internes que lui-même ne voit pas.

**Exemple d'attaque.** Un champ « importer une photo depuis cette adresse » reçoit `http://localhost:3306` ou
l'adresse d'un service interne.

**Dans ImmoConnect.** Le risque est écarté par conception : **aucune fonction ne télécharge une adresse fournie
par un utilisateur**. Une photo se téléverse comme un fichier, jamais par un lien.

Le serveur n'appelle que quatre destinations, toutes fixées par le code ou par la configuration du serveur :

- l'API de Stripe, pour les paiements ;
- `api.pwnedpasswords.com`, pour vérifier les mots de passe compromis ;
- le serveur de messagerie, pour envoyer les e-mails (`SMTP_HOST`) ;
- le conteneur du frontend, sur le réseau interne, pour le rendu des pages publiques (`RENDU_COQUILLE`).

---

## Et l'édition 2025 ?

L'OWASP a publié une édition 2025. Les mêmes familles y figurent, avec trois changements utiles à connaître :

- la SSRF (A10) est rangée dans le contrôle d'accès (A01) ;
- « composants vulnérables » s'élargit en « défaillances de la chaîne d'approvisionnement logicielle » ;
- une nouvelle catégorie apparaît, la mauvaise gestion des cas exceptionnels (erreurs mal traitées). ImmoConnect y
  répond par son gestionnaire d'erreurs central, qui donne à chaque erreur prévue une réponse propre et ne laisse
  sortir aucune trace technique.

Le rapport étant fondé sur l'édition 2021, c'est elle qui sert de référence ici.
