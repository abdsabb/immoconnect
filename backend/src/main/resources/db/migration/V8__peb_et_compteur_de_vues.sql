-- Certificat PEB structuré (obligatoire dans toute publicité immobilière, chapitre 11 du rapport)
-- et compteur de vues de la fiche publique (tableau de bord de l'agent, cas AG6).

ALTER TABLE bien
  ADD COLUMN peb ENUM('A++','A+','A','B','C','D','E','F','G') NOT NULL DEFAULT 'D' AFTER nb_chambres,
  ADD COLUMN nb_vues INT UNSIGNED NOT NULL DEFAULT 0 AFTER publie_le;

-- Les descriptions des données de test mentionnent déjà « PEB x. » en texte libre : on en fait la valeur du champ
UPDATE bien
   SET peb = REGEXP_SUBSTR(description, '(?<=PEB )A\\+\\+|(?<=PEB )A\\+|(?<=PEB )[A-G]')
 WHERE description REGEXP 'PEB (A\\+\\+|A\\+|[A-G])[ .,]';

-- Vues de démonstration : plus un bien est ancien et apprécié, plus il a été vu ; reproductible d'une base à l'autre
UPDATE bien b
  LEFT JOIN (SELECT bien_id, COUNT(*) AS n FROM favori GROUP BY bien_id) f ON f.bien_id = b.id
   SET b.nb_vues = 20 + (b.id * 37) % 180 + 6 * COALESCE(f.n, 0) + LEAST(DATEDIFF(CURRENT_DATE, b.publie_le), 365) DIV 3;

ALTER TABLE bien ALTER COLUMN peb DROP DEFAULT;
