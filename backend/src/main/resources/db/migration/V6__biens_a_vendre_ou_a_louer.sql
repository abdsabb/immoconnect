-- ImmoConnect — V6 : un bien est à vendre ou à louer.
-- Jusqu'ici, rien ne distinguait les deux : une location se reconnaissait à « (location) » dans son
-- titre, et son loyer occupait la colonne prix. La nature de l'offre devient une donnée du modèle.
-- Pour une location, la colonne prix contient le loyer mensuel.

ALTER TABLE bien
  ADD COLUMN type_offre ENUM('vente','location') NOT NULL DEFAULT 'vente' AFTER categorie_id,
  ADD KEY idx_bien_type_offre (type_offre);

-- 1. Les locations des données de test : la mention quitte le titre
UPDATE bien
   SET type_offre = 'location',
       titre = TRIM(REPLACE(titre, ' (location)', ''))
 WHERE titre LIKE '% (location)';

-- 2. Une location ne peut pas être « vendue »
UPDATE bien SET statut = 'loue' WHERE type_offre = 'location' AND statut = 'vendu';

-- 3. Le loyer d'un garage suit sa surface (un garage de 20 m² était affiché à 2 550 € par mois).
--    MySQL applique les affectations de gauche à droite : la description est corrigée avec l'ancien loyer.
UPDATE bien
   SET description = REPLACE(description,
                             CONCAT('Loyer de ', CAST(prix AS UNSIGNED), ' €'),
                             CONCAT('Loyer de ', CAST(ROUND((60 + superficie * 1.5) / 5) * 5 AS UNSIGNED), ' €')),
       prix = ROUND((60 + superficie * 1.5) / 5) * 5
 WHERE type_offre = 'location' AND categorie_id = 9;

-- 4. Seules trois locations étaient disponibles : deux logements en ligne sur cinq passent à la location.
--    Loyer arrondi à 25 € : une base, plus la surface et le nombre de chambres.
UPDATE bien
   SET type_offre = 'location',
       prix = LEAST(3500, ROUND((400 + superficie * 8 + nb_chambres * 60) / 25) * 25),
       description = CONCAT(description, ' Loyer de ', CAST(prix AS UNSIGNED), ' € par mois, charges comprises.')
 WHERE type_offre = 'vente'
   AND statut IN ('disponible', 'sous_option')
   AND categorie_id IN (1, 2, 3, 4, 5)
   AND MOD(id, 5) IN (1, 3);

-- 5. Une annonce de location ne parle ni d'achat, ni d'acte, ni de rendement
UPDATE bien
   SET description = REPLACE(REPLACE(REPLACE(description,
           'Idéal pour un premier achat.', 'Idéal pour un premier logement.'),
           'Libre à l''acte.', 'Libre immédiatement.'),
           'Excellent rendement locatif.', 'Proche des transports.')
 WHERE type_offre = 'location';

-- 6. Règle du domaine : « loué » ne concerne qu'une location, « vendu » qu'une vente
ALTER TABLE bien
  ADD CONSTRAINT chk_bien_offre_statut
  CHECK ((type_offre = 'vente' AND statut <> 'loue') OR (type_offre = 'location' AND statut <> 'vendu'));
