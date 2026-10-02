-- Suite de V13 : Tervuren, Wavre, Mons, Waterloo et Charleroi ne sont pas non plus des communes bruxelloises.
-- Leurs annonces de test rejoignent cinq communes de la Région ; elles ne sont pas supprimées, pour garder
-- cohérents leurs rendez-vous, paiements et favoris. Les articles du blog qui citaient une ville hors Région
-- (ces cinq-là et les quatre de V13) parlent désormais de la commune correspondante.

UPDATE bien
   SET titre = REPLACE(titre, '— Tervuren', '— Auderghem'),
       description = REPLACE(description, ' à Tervuren.', ' à Auderghem.'),
       ville = 'Auderghem', code_postal = '1160',
       latitude = 50.815600 + (CAST((id * 37) % 21 AS SIGNED) - 10) * 0.000300,
       longitude = 4.433100 + (CAST((id * 53) % 21 AS SIGNED) - 10) * 0.000400
 WHERE ville = 'Tervuren';

UPDATE bien
   SET titre = REPLACE(titre, '— Wavre', '— Uccle'),
       description = REPLACE(description, ' à Wavre.', ' à Uccle.'),
       ville = 'Uccle', code_postal = '1180',
       latitude = 50.802000 + (CAST((id * 37) % 21 AS SIGNED) - 10) * 0.000300,
       longitude = 4.339000 + (CAST((id * 53) % 21 AS SIGNED) - 10) * 0.000400
 WHERE ville = 'Wavre';

UPDATE bien
   SET titre = REPLACE(titre, '— Mons', '— Anderlecht'),
       description = REPLACE(description, ' à Mons.', ' à Anderlecht.'),
       ville = 'Anderlecht', code_postal = '1070',
       latitude = 50.836600 + (CAST((id * 37) % 21 AS SIGNED) - 10) * 0.000300,
       longitude = 4.308000 + (CAST((id * 53) % 21 AS SIGNED) - 10) * 0.000400
 WHERE ville = 'Mons';

UPDATE bien
   SET titre = REPLACE(titre, '— Waterloo', '— Schaerbeek'),
       description = REPLACE(description, ' à Waterloo.', ' à Schaerbeek.'),
       ville = 'Schaerbeek', code_postal = '1030',
       latitude = 50.867600 + (CAST((id * 37) % 21 AS SIGNED) - 10) * 0.000300,
       longitude = 4.373700 + (CAST((id * 53) % 21 AS SIGNED) - 10) * 0.000400
 WHERE ville = 'Waterloo';

UPDATE bien
   SET titre = REPLACE(titre, '— Charleroi', '— Saint-Gilles'),
       description = REPLACE(description, ' à Charleroi.', ' à Saint-Gilles.'),
       ville = 'Saint-Gilles', code_postal = '1060',
       latitude = 50.826700 + (CAST((id * 37) % 21 AS SIGNED) - 10) * 0.000300,
       longitude = 4.346000 + (CAST((id * 53) % 21 AS SIGNED) - 10) * 0.000400
 WHERE ville = 'Charleroi';

-- Blog : « à Mons » devient « à Anderlecht », etc. (les noms de rues, comme « chaussée de Waterloo », ne changent pas)
UPDATE article SET titre = REPLACE(titre, ' à Tervuren', ' à Auderghem'), contenu = REPLACE(contenu, ' à Tervuren', ' à Auderghem') WHERE titre LIKE '% à Tervuren%' OR contenu LIKE '% à Tervuren%';
UPDATE article SET titre = REPLACE(titre, ' à Wavre', ' à Uccle'), contenu = REPLACE(contenu, ' à Wavre', ' à Uccle') WHERE titre LIKE '% à Wavre%' OR contenu LIKE '% à Wavre%';
UPDATE article SET titre = REPLACE(titre, ' à Mons', ' à Anderlecht'), contenu = REPLACE(contenu, ' à Mons', ' à Anderlecht') WHERE titre LIKE '% à Mons%' OR contenu LIKE '% à Mons%';
UPDATE article SET titre = REPLACE(titre, ' à Waterloo', ' à Schaerbeek'), contenu = REPLACE(contenu, ' à Waterloo', ' à Schaerbeek') WHERE titre LIKE '% à Waterloo%' OR contenu LIKE '% à Waterloo%';
UPDATE article SET titre = REPLACE(titre, ' à Charleroi', ' à Saint-Gilles'), contenu = REPLACE(contenu, ' à Charleroi', ' à Saint-Gilles') WHERE titre LIKE '% à Charleroi%' OR contenu LIKE '% à Charleroi%';
UPDATE article SET titre = REPLACE(titre, ' à Namur', ' à Koekelberg'), contenu = REPLACE(contenu, ' à Namur', ' à Koekelberg') WHERE titre LIKE '% à Namur%' OR contenu LIKE '% à Namur%';
UPDATE article SET titre = REPLACE(titre, ' à Nivelles', ' à Ganshoren'), contenu = REPLACE(contenu, ' à Nivelles', ' à Ganshoren') WHERE titre LIKE '% à Nivelles%' OR contenu LIKE '% à Nivelles%';
UPDATE article SET titre = REPLACE(titre, ' à Liège', ' à Saint-Josse-ten-Noode'), contenu = REPLACE(contenu, ' à Liège', ' à Saint-Josse-ten-Noode') WHERE titre LIKE '% à Liège%' OR contenu LIKE '% à Liège%';
UPDATE article SET titre = REPLACE(titre, ' à Louvain-la-Neuve', ' à Berchem-Sainte-Agathe'), contenu = REPLACE(contenu, ' à Louvain-la-Neuve', ' à Berchem-Sainte-Agathe') WHERE titre LIKE '% à Louvain-la-Neuve%' OR contenu LIKE '% à Louvain-la-Neuve%';
