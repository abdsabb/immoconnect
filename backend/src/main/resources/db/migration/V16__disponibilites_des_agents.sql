-- Disponibilités des agents : un agent ouvre lui-même les plages pendant lesquelles il reçoit en visite.
-- Un créneau n'est proposé aux membres que s'il tombe dans une plage ouverte par l'agent du bien : sans plage,
-- aucun créneau. Une plage tient dans une seule journée ; l'agent la retire s'il n'est plus disponible.

CREATE TABLE disponibilite (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  agent_id INT UNSIGNED NOT NULL,
  debut DATETIME NOT NULL,
  fin DATETIME NOT NULL,
  cree_le DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_disponibilite_agent_debut (agent_id, debut),
  CONSTRAINT fk_disponibilite_agent FOREIGN KEY (agent_id) REFERENCES agent_immobilier (utilisateur_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT ck_disponibilite_periode CHECK (fin > debut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Les plages des agents de test dépendent de la date du jour : elles sont créées au démarrage de l'application
-- (DisponibilitesDeDemonstration), pas par cette migration.
