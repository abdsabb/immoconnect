-- Formulaire de contact du site : chaque message est enregistré, envoyé par e-mail à l'agence et suivi dans le
-- back-office. L'auteur n'a pas besoin de compte : la demande porte ses coordonnées, pas un utilisateur.

CREATE TABLE demande_contact (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nom VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL,
  telephone VARCHAR(30) NULL,
  sujet VARCHAR(150) NOT NULL,
  message TEXT NOT NULL,
  cree_le DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip VARCHAR(45) NOT NULL,
  traite_le DATETIME NULL,
  traite_par INT UNSIGNED NULL,
  PRIMARY KEY (id),
  KEY idx_contact_date (cree_le),
  CONSTRAINT fk_contact_administrateur FOREIGN KEY (traite_par) REFERENCES administrateur (utilisateur_id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Demandes de démonstration : une traitée par la gestionnaire, une en attente
INSERT INTO demande_contact (nom, email, telephone, sujet, message, cree_le, ip, traite_le, traite_par) VALUES
('Claire Lambert', 'claire.lambert@mail.be', '+32 475 11 22 33', 'Estimation d’un appartement à Ixelles',
 'Bonjour, je souhaite mettre en vente mon appartement de deux chambres près de la place Flagey. Proposez-vous une estimation gratuite ?',
 DATE_SUB(NOW(), INTERVAL 6 DAY), '81.82.14.201', DATE_SUB(NOW(), INTERVAL 5 DAY), 110),
('Thomas Janssens', 'thomas.janssens@mail.be', NULL, 'Mise en location d’une maison',
 'Bonjour, je pars à l’étranger pour deux ans et j’aimerais confier la location de ma maison à Uccle. Quels sont vos honoraires de gestion ?',
 DATE_SUB(NOW(), INTERVAL 1 DAY), '91.176.40.12', NULL, NULL);
