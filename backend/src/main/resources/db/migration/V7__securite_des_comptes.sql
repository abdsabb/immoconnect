-- ImmoConnect — V7 : sécurité des comptes (livrable 16, §2).
--   * verrouillage progressif après des échecs de connexion ;
--   * adresse e-mail confirmée par un lien d'activation ;
--   * double facteur : code envoyé par e-mail, obligatoire pour les agents et les administrateurs ;
--   * acceptation des conditions générales et consentement aux communications ;
--   * jetons à usage unique, dont seule l'empreinte est enregistrée.

ALTER TABLE utilisateur
  ADD COLUMN email_verifie TINYINT(1) NOT NULL DEFAULT 0 AFTER actif,
  ADD COLUMN double_facteur TINYINT(1) NOT NULL DEFAULT 0 AFTER email_verifie,
  ADD COLUMN echecs_connexion TINYINT UNSIGNED NOT NULL DEFAULT 0 AFTER double_facteur,
  ADD COLUMN verrouille_jusqu_a DATETIME NULL AFTER echecs_connexion,
  ADD COLUMN cgu_acceptees_le DATETIME NULL AFTER verrouille_jusqu_a,
  ADD COLUMN consentement_communications TINYINT(1) NOT NULL DEFAULT 0 AFTER cgu_acceptees_le;

-- Les comptes existants ont été créés avant ces règles : adresse réputée confirmée, conditions
-- réputées acceptées à l'inscription. Un membre sur quatre avait accepté les communications.
UPDATE utilisateur
   SET email_verifie = 1,
       cgu_acceptees_le = TIMESTAMP(date_inscription, '09:00:00'),
       consentement_communications = IF(role = 'membre' AND MOD(id, 4) = 0, 1, 0);

-- Le double facteur est imposé par le rôle ; un membre peut le choisir (un sur dix l'a fait).
UPDATE utilisateur SET double_facteur = 1 WHERE role IN ('agent', 'admin') OR MOD(id, 10) = 0;

CREATE TABLE jeton (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  utilisateur_id INT UNSIGNED NOT NULL,
  type ENUM('rafraichissement','activation','reinitialisation','double_facteur') NOT NULL,
  empreinte CHAR(64) NOT NULL,
  empreinte_code CHAR(64) NULL,
  essais TINYINT UNSIGNED NOT NULL DEFAULT 0,
  cree_le DATETIME NOT NULL,
  expire_le DATETIME NOT NULL,
  utilise_le DATETIME NULL,
  ip VARCHAR(45) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_jeton_empreinte (empreinte),
  KEY idx_jeton_utilisateur (utilisateur_id, type),
  KEY idx_jeton_expiration (expire_le),
  CONSTRAINT fk_jeton_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT chk_jeton_dates CHECK (expire_le > cree_le)
) ENGINE=InnoDB;

-- Données de test : chaque journée d'activité d'un utilisateur au journal d'audit correspond à une
-- session de quatorze jours, aujourd'hui expirée ou fermée par une déconnexion. L'empreinte est
-- celle d'une valeur que personne ne connaît : aucune de ces sessions ne peut être reprise.
INSERT INTO jeton (utilisateur_id, type, empreinte, cree_le, expire_le, utilise_le, ip)
SELECT s.utilisateur_id, 'rafraichissement', SHA2(CONCAT('session-de-test-', s.premiere), 256),
       s.debut, DATE_ADD(s.debut, INTERVAL 14 DAY),
       IF(MOD(s.premiere, 3) = 0, s.fin, NULL), s.ip
  FROM (SELECT utilisateur_id, MIN(id) AS premiere, MIN(horodatage) AS debut,
               DATE_ADD(MAX(horodatage), INTERVAL 20 MINUTE) AS fin, MIN(ip) AS ip
          FROM journal_audit
         GROUP BY utilisateur_id, DATE(horodatage)) s;

-- Les agents et les administrateurs ont reçu un code à chaque connexion : tous utilisés ou expirés
INSERT INTO jeton (utilisateur_id, type, empreinte, empreinte_code, essais, cree_le, expire_le, utilise_le, ip)
SELECT s.utilisateur_id, 'double_facteur', SHA2(CONCAT('defi-de-test-', s.premiere), 256),
       SHA2(CONCAT('code-de-test-', s.premiere), 256), MOD(s.premiere, 3),
       DATE_SUB(s.debut, INTERVAL 1 MINUTE), DATE_ADD(s.debut, INTERVAL 9 MINUTE),
       IF(MOD(s.premiere, 7) = 0, NULL, s.debut), s.ip
  FROM (SELECT j.utilisateur_id, MIN(j.id) AS premiere, MIN(j.horodatage) AS debut, MIN(j.ip) AS ip
          FROM journal_audit j JOIN utilisateur u ON u.id = j.utilisateur_id
         WHERE u.role IN ('agent', 'admin')
         GROUP BY j.utilisateur_id, DATE(j.horodatage)) s;

-- Quelques membres ont demandé à réinitialiser leur mot de passe
INSERT INTO jeton (utilisateur_id, type, empreinte, cree_le, expire_le, utilise_le, ip)
SELECT j.utilisateur_id, 'reinitialisation', SHA2(CONCAT('reinitialisation-de-test-', j.id), 256),
       DATE_SUB(j.horodatage, INTERVAL 6 MINUTE), DATE_ADD(j.horodatage, INTERVAL 24 MINUTE),
       IF(MOD(j.id, 4) = 0, NULL, j.horodatage), j.ip
  FROM journal_audit j
 WHERE j.action = 'connexion';
