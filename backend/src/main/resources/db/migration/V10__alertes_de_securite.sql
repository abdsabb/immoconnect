-- Alertes de la détection d'intrusion (chapitre 9.6 du rapport) : rafale d'échecs de connexion,
-- énumération d'identifiants, usage d'une clé API révoquée. Une alerte n'est liée à aucun compte :
-- elle décrit une adresse IP, pas un utilisateur — d'où une table à part du journal d'audit.

CREATE TABLE alerte_securite (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  type ENUM('rafale_echecs','enumeration','cle_revoquee') NOT NULL,
  ip VARCHAR(45) NOT NULL,
  detail VARCHAR(255) NOT NULL,
  cree_le DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_alerte_date (cree_le),
  KEY idx_alerte_ip (ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Alertes de démonstration
INSERT INTO alerte_securite (type, ip, detail, cree_le) VALUES
('rafale_echecs', '185.220.101.47', '12 échecs de connexion en 10 min', DATE_SUB(NOW(), INTERVAL 9 DAY)),
('enumeration', '45.155.205.233', '7 comptes différents essayés en 10 min', DATE_SUB(NOW(), INTERVAL 4 DAY)),
('cle_revoquee', '91.183.62.18', 'clé « Portail partenaire (ancienne) »', DATE_SUB(NOW(), INTERVAL 1 DAY));
