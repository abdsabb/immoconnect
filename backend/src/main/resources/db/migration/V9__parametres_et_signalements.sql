-- Paramètres du site gérés par l'administrateur (cas A5 : nom de l'agence, coordonnées, langues actives)
-- et signalement de contenus par les utilisateurs (règlement européen sur les services numériques, chapitre 11).

CREATE TABLE parametre (
  cle VARCHAR(60) NOT NULL,
  valeur VARCHAR(1000) NOT NULL,
  modifie_le DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modifie_par INT UNSIGNED NULL,
  PRIMARY KEY (cle),
  CONSTRAINT fk_parametre_administrateur FOREIGN KEY (modifie_par) REFERENCES administrateur (utilisateur_id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO parametre (cle, valeur) VALUES
('agence.nom', 'ImmoConnect'),
('agence.slogan', 'Votre agence immobilière à Bruxelles'),
('agence.adresse', 'Avenue Louise 143, 1050 Bruxelles'),
('agence.telephone', '+32 2 555 12 34'),
('agence.email', 'contact@immoconnect.be'),
('agence.horaires', 'Lundi – vendredi : 9 h – 18 h ; samedi : 10 h – 13 h'),
('langues.actives', 'fr,nl,en');

CREATE TABLE signalement (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  auteur_id INT UNSIGNED NOT NULL,
  type_contenu ENUM('message','bien','article') NOT NULL,
  contenu_id INT UNSIGNED NOT NULL,
  motif ENUM('illicite','arnaque','indesirable','autre') NOT NULL,
  description VARCHAR(1000) NOT NULL,
  statut ENUM('ouvert','retire','conserve') NOT NULL DEFAULT 'ouvert',
  cree_le DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  traite_le DATETIME NULL,
  traite_par INT UNSIGNED NULL,
  decision VARCHAR(500) NULL,
  PRIMARY KEY (id),
  KEY idx_signalement_statut (statut, cree_le),
  KEY idx_signalement_contenu (type_contenu, contenu_id),
  CONSTRAINT fk_signalement_auteur FOREIGN KEY (auteur_id) REFERENCES utilisateur (id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_signalement_administrateur FOREIGN KEY (traite_par) REFERENCES administrateur (utilisateur_id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT chk_signalement_traitement CHECK ((statut = 'ouvert') = (traite_le IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Deux signalements de démonstration : un traité par la gestionnaire, un encore ouvert
INSERT INTO signalement (auteur_id, type_contenu, contenu_id, motif, description, statut, cree_le, traite_le, traite_par, decision)
SELECT msg.agent_id, 'message', msg.id, 'indesirable',
       'Ce message ne concerne pas une visite : il me propose un « investissement » sans rapport avec le bien.',
       'conserve', DATE_ADD(msg.envoye_le, INTERVAL 1 DAY), DATE_ADD(msg.envoye_le, INTERVAL 3 DAY), 110,
       'Message relu : demande d’information maladroite mais sans caractère illicite. Aucun retrait.'
  FROM message msg
 ORDER BY msg.id LIMIT 1;

INSERT INTO signalement (auteur_id, type_contenu, contenu_id, motif, description, cree_le)
SELECT f.membre_id, 'bien', f.bien_id, 'arnaque',
       'Le loyer annoncé est très en dessous du marché pour ce quartier et la description ressemble à une autre annonce.',
       DATE_SUB(NOW(), INTERVAL 2 DAY)
  FROM favori f JOIN bien b ON b.id = f.bien_id AND b.type_offre = 'location'
 ORDER BY f.bien_id, f.membre_id LIMIT 1;
