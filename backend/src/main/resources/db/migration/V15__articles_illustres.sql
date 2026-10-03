-- Blog illustré : chaque article peut porter une image de couverture, téléversée par l'administrateur.
-- Les articles de test reçoivent une couverture /storage/articles/{id}/couverture.jpg, créée au démarrage à partir
-- du jeu de photos libres embarqué (comme les photos des annonces de test).
ALTER TABLE article ADD COLUMN image_url VARCHAR(255) NULL AFTER contenu;

UPDATE article SET image_url = CONCAT('/storage/articles/', id, '/couverture.jpg');

-- Les articles de test tenaient en un seul paragraphe, le même pour tous. Ils reçoivent un texte structuré selon leur
-- catégorie : chapeau, intertitres (ligne « ## ») et listes (lignes « - »). Un article rédigé depuis le back-office
-- ne contient pas la phrase type : il n'est pas touché.

UPDATE article SET contenu = CONCAT(
  'Acheter un logement est souvent le projet d’une vie. Avant de signer, mieux vaut connaître les étapes, le budget réel et les points à vérifier pendant la visite. Voici la méthode que nos agents recommandent à chaque candidat acquéreur.',
  '\n\n## Définir son budget réel\n\n',
  'Au prix du bien s’ajoutent les droits d’enregistrement, les frais de notaire et les frais liés au crédit. En Région bruxelloise, les droits d’enregistrement s’élèvent à 12,5 % du prix, avec un abattement possible pour l’achat d’une habitation propre et unique. Demandez une simulation à votre banque avant la première visite : vous saurez jusqu’où vous pouvez aller.',
  '\n\n## Visiter avec méthode\n\n',
  '- Vérifiez le certificat PEB et l’âge de la chaudière.\n- Regardez l’état de la toiture, des châssis et de l’installation électrique.\n- Demandez le montant des charges et, en copropriété, les derniers procès-verbaux d’assemblée générale.\n- Revenez à une autre heure pour juger du bruit et de la lumière.',
  '\n\n## De l’offre à l’acte\n\n',
  'Une offre écrite et acceptée engage les deux parties. Le compromis est signé dans les semaines qui suivent, puis l’acte authentique chez le notaire, en général dans les quatre mois. Entre-temps, le notaire vérifie la situation urbanistique et hypothécaire du bien.',
  '\n\nUn doute sur un bien ? Réservez une visite en ligne : nos agents répondent à vos questions sur place.')
 WHERE categorie_article_id = 1 AND contenu LIKE '%Dans cet article, notre équipe fait le point%';

UPDATE article SET contenu = CONCAT(
  'Louer un logement à Bruxelles demande un dossier solide et quelques réflexes. Bail, garantie locative, état des lieux : voici ce qu’il faut savoir avant de signer, pour emménager l’esprit tranquille et éviter les mauvaises surprises à la sortie.',
  '\n\n## Préparer son dossier\n\n',
  'Préparez une pièce d’identité et une preuve de vos revenus, par exemple vos trois dernières fiches de paie. Un dossier complet, remis dès la visite, fait souvent la différence quand plusieurs candidats se présentent pour le même logement.',
  '\n\n## Le bail et la garantie locative\n\n',
  '- Le bail de résidence principale est conclu par écrit et enregistré par le bailleur.\n- La garantie locative placée sur un compte bloqué ne dépasse pas deux mois de loyer.\n- Le loyer peut être indexé une fois par an, à la date anniversaire du bail.\n- Les charges sont détaillées : demandez ce qu’elles couvrent.',
  '\n\n## L’état des lieux\n\n',
  'Un état des lieux d’entrée détaillé, daté et signé par les deux parties protège le locataire comme le bailleur. Photographiez chaque pièce et relevez les compteurs le jour de la remise des clés.',
  '\n\nNos biens à louer sont visibles en ligne avec leur certificat PEB. Demandez une visite en quelques clics.')
 WHERE categorie_article_id = 2 AND contenu LIKE '%Dans cet article, notre équipe fait le point%';

UPDATE article SET contenu = CONCAT(
  'Prix, délais de vente, quartiers recherchés : le marché bruxellois évolue d’un trimestre à l’autre. Notre équipe fait le point à partir des biens qu’elle suit au quotidien, pour aider vendeurs et acheteurs à se situer avant de se lancer.',
  '\n\n## Les prix\n\n',
  'Les prix varient fortement d’une commune à l’autre, et parfois d’une rue à l’autre. Les appartements bien situés, proches des transports et dotés d’un bon PEB, se vendent le plus vite. Les biens à rénover se négocient davantage, car les acheteurs intègrent le coût des travaux.',
  '\n\n## Les délais\n\n',
  'Un bien affiché au juste prix trouve preneur en quelques semaines. Un prix trop ambitieux allonge le délai et finit souvent par une baisse. Une estimation réaliste dès le départ reste le meilleur moyen de vendre vite.',
  '\n\n## Ce que cherchent les acheteurs\n\n',
  '- Un extérieur, même petit : terrasse, balcon ou jardin.\n- Une bonne performance énergétique.\n- Un espace pour travailler à domicile.\n- La proximité des écoles et des transports.',
  '\n\nVous souhaitez connaître la valeur de votre bien ? Écrivez-nous par le formulaire de contact : l’estimation est gratuite.')
 WHERE categorie_article_id = 3 AND contenu LIKE '%Dans cet article, notre équipe fait le point%';

UPDATE article SET contenu = CONCAT(
  'Le certificat PEB pèse de plus en plus dans le prix d’un logement. Bien rénover, c’est gagner en confort, réduire ses factures et valoriser son bien. Encore faut-il engager les travaux dans le bon ordre et connaître les aides disponibles.',
  '\n\n## Comprendre le certificat PEB\n\n',
  'Le certificat classe le logement de A à G selon sa consommation d’énergie. Il est obligatoire pour vendre ou louer, et sa classe doit figurer dans l’annonce. Sur ImmoConnect, elle apparaît sur chaque fiche.',
  '\n\n## Par où commencer\n\n',
  '- Isoler la toiture : c’est souvent le poste le plus rentable.\n- Remplacer les châssis à simple vitrage.\n- Moderniser le chauffage et sa régulation.\n- Soigner la ventilation pour éviter l’humidité.',
  '\n\n## Financer les travaux\n\n',
  'Des primes régionales et des prêts à taux réduit existent pour les travaux d’économie d’énergie. Leurs conditions changent régulièrement : renseignez-vous auprès de la Région avant de signer un devis, et conservez toutes vos factures.',
  '\n\nPour chaque bien, nos agents vous indiquent les travaux à prévoir et leur ordre de priorité.')
 WHERE categorie_article_id = 4 AND contenu LIKE '%Dans cet article, notre équipe fait le point%';

UPDATE article SET contenu = CONCAT(
  'Droits d’enregistrement, frais de notaire, taux du crédit : le coût d’un achat ne se résume pas au prix affiché. Voici les postes à prévoir pour établir un plan de financement réaliste, avant même de faire une offre.',
  '\n\n## Les frais d’acquisition\n\n',
  'En Région bruxelloise, les droits d’enregistrement représentent 12,5 % du prix d’achat ; un abattement réduit la base taxable pour l’achat d’une habitation propre et unique, sous conditions. S’y ajoutent les honoraires du notaire et les frais administratifs.',
  '\n\n## Le crédit hypothécaire\n\n',
  '- Comparez plusieurs banques : le taux n’est pas le seul critère.\n- Regardez aussi l’assurance solde restant dû et les frais de dossier.\n- Un apport personnel couvrant au moins les frais est généralement attendu.\n- Gardez une réserve pour les imprévus et les premiers travaux.',
  '\n\n## Taux fixe ou variable\n\n',
  'Le taux fixe garantit une mensualité identique jusqu’à la fin du prêt. Le taux variable peut démarrer plus bas, mais il évolue avec le marché, dans les limites prévues au contrat. Le bon choix dépend de la durée du prêt et de votre marge de sécurité.',
  '\n\nCes informations sont générales : votre notaire et votre banque vous donneront les montants exacts pour votre situation.')
 WHERE categorie_article_id = 5 AND contenu LIKE '%Dans cet article, notre équipe fait le point%';

UPDATE article SET contenu = CONCAT(
  'ImmoConnect accompagne vendeurs, acheteurs et locataires à Bruxelles. Dans cette rubrique, l’agence partage ses nouveautés et les coulisses de son travail : la façon dont elle suit ses annonces, organise ses visites et répond à ses clients.',
  '\n\n## Une équipe de terrain\n\n',
  'Nos agents connaissent les quartiers qu’ils couvrent : ils y visitent, estiment et vendent des biens toute l’année. Chaque annonce est suivie par un agent attitré, joignable par la messagerie du site.',
  '\n\n## Des visites réservées en ligne\n\n',
  '- Choisissez un créneau directement dans l’agenda de l’agent.\n- Recevez une confirmation par e-mail.\n- Optez pour un créneau premium si votre agenda est chargé.\n- Annulez votre visite depuis votre espace en cas d’empêchement.',
  '\n\n## À votre écoute\n\n',
  'Une question, un projet de vente ou de mise en location ? L’agence répond par e-mail aux demandes envoyées par le formulaire de contact.',
  '\n\nMerci de votre confiance, et à bientôt lors d’une visite.')
 WHERE categorie_article_id = 6 AND contenu LIKE '%Dans cet article, notre équipe fait le point%';
