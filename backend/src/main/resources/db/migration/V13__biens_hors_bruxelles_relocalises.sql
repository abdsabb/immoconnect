-- L'agence travaille à Bruxelles : Namur, Nivelles, Liège et Louvain-la-Neuve ne sont pas des communes bruxelloises.
-- Les annonces de test qui s'y trouvaient sont relocalisées dans quatre communes de la Région qui n'en avaient
-- aucune. Elles ne sont pas supprimées : leurs rendez-vous, paiements et favoris restent cohérents.
-- La rue garde son nom ; la position est celle du centre de la commune, décalée de quelques rues selon l'annonce.

UPDATE bien
   SET titre = REPLACE(titre, '— Namur', '— Koekelberg'),
       description = REPLACE(description, ' à Namur.', ' à Koekelberg.'),
       ville = 'Koekelberg', code_postal = '1081',
       latitude = 50.862200 + (CAST((id * 37) % 21 AS SIGNED) - 10) * 0.000300,
       longitude = 4.330600 + (CAST((id * 53) % 21 AS SIGNED) - 10) * 0.000400
 WHERE ville = 'Namur';

UPDATE bien
   SET titre = REPLACE(titre, '— Nivelles', '— Ganshoren'),
       description = REPLACE(description, ' à Nivelles.', ' à Ganshoren.'),
       ville = 'Ganshoren', code_postal = '1083',
       latitude = 50.871700 + (CAST((id * 37) % 21 AS SIGNED) - 10) * 0.000300,
       longitude = 4.310300 + (CAST((id * 53) % 21 AS SIGNED) - 10) * 0.000400
 WHERE ville = 'Nivelles';

UPDATE bien
   SET titre = REPLACE(titre, '— Liège', '— Saint-Josse-ten-Noode'),
       description = REPLACE(description, ' à Liège.', ' à Saint-Josse-ten-Noode.'),
       ville = 'Saint-Josse-ten-Noode', code_postal = '1210',
       latitude = 50.853600 + (CAST((id * 37) % 21 AS SIGNED) - 10) * 0.000200,
       longitude = 4.370300 + (CAST((id * 53) % 21 AS SIGNED) - 10) * 0.000300
 WHERE ville = 'Liège';

UPDATE bien
   SET titre = REPLACE(titre, '— Louvain-la-Neuve', '— Berchem-Sainte-Agathe'),
       description = REPLACE(description, ' à Louvain-la-Neuve.', ' à Berchem-Sainte-Agathe.'),
       ville = 'Berchem-Sainte-Agathe', code_postal = '1082',
       latitude = 50.865000 + (CAST((id * 37) % 21 AS SIGNED) - 10) * 0.000300,
       longitude = 4.294000 + (CAST((id * 53) % 21 AS SIGNED) - 10) * 0.000400
 WHERE ville = 'Louvain-la-Neuve';
