-- ============================================================
-- ImmoConnect — Schéma de la base de données (livrable 08/09)
-- MySQL 8.0 · InnoDB · utf8mb4
-- ============================================================
DROP DATABASE IF EXISTS immoconnect;
CREATE DATABASE immoconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE immoconnect;

-- ---------- Tables de référence ----------
CREATE TABLE langue (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  code CHAR(2) NOT NULL,
  nom VARCHAR(40) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_langue_code (code),
  CONSTRAINT chk_langue_code CHECK (code REGEXP '^[a-z]{2}$')
) ENGINE=InnoDB;

CREATE TABLE categorie (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nom VARCHAR(60) NOT NULL,
  description TEXT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_categorie_nom (nom)
) ENGINE=InnoDB;

CREATE TABLE categorie_article (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nom VARCHAR(60) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_categorie_article_nom (nom)
) ENGINE=InnoDB;

-- ---------- Utilisateurs (héritage : table mère + tables filles) ----------
CREATE TABLE utilisateur (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nom VARCHAR(80) NOT NULL,
  prenom VARCHAR(80) NOT NULL,
  email VARCHAR(190) NOT NULL,
  mot_de_passe VARCHAR(255) NOT NULL,
  langue_id INT UNSIGNED NOT NULL,
  photo_url VARCHAR(255) NULL,
  date_inscription DATE NOT NULL,
  role ENUM('membre','agent','admin') NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_utilisateur_email (email),
  KEY idx_utilisateur_langue (langue_id),
  CONSTRAINT fk_utilisateur_langue FOREIGN KEY (langue_id) REFERENCES langue (id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE membre (
  utilisateur_id INT UNSIGNED NOT NULL,
  telephone VARCHAR(20) NULL,
  PRIMARY KEY (utilisateur_id),
  CONSTRAINT fk_membre_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE agent_immobilier (
  utilisateur_id INT UNSIGNED NOT NULL,
  matricule VARCHAR(20) NOT NULL,
  telephone_pro VARCHAR(20) NOT NULL,
  PRIMARY KEY (utilisateur_id),
  UNIQUE KEY uq_agent_matricule (matricule),
  CONSTRAINT fk_agent_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE administrateur (
  utilisateur_id INT UNSIGNED NOT NULL,
  niveau_acces TINYINT UNSIGNED NOT NULL,
  PRIMARY KEY (utilisateur_id),
  CONSTRAINT fk_admin_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT chk_admin_niveau CHECK (niveau_acces BETWEEN 1 AND 3)
) ENGINE=InnoDB;

-- ---------- Biens et photos ----------
CREATE TABLE bien (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  agent_id INT UNSIGNED NOT NULL,
  categorie_id INT UNSIGNED NOT NULL,
  titre VARCHAR(150) NOT NULL,
  description TEXT NOT NULL,
  prix DECIMAL(12,2) NOT NULL,
  superficie DECIMAL(8,2) NOT NULL,
  nb_chambres TINYINT UNSIGNED NOT NULL,
  adresse VARCHAR(150) NOT NULL,
  ville VARCHAR(80) NOT NULL,
  code_postal VARCHAR(10) NOT NULL,
  latitude DECIMAL(9,6) NOT NULL,
  longitude DECIMAL(9,6) NOT NULL,
  statut ENUM('disponible','sous_option','vendu','loue','archive') NOT NULL DEFAULT 'disponible',
  publie_le DATE NOT NULL,
  PRIMARY KEY (id),
  KEY idx_bien_agent (agent_id),
  KEY idx_bien_categorie (categorie_id),
  KEY idx_bien_statut (statut),
  KEY idx_bien_ville (ville),
  KEY idx_bien_prix (prix),
  CONSTRAINT fk_bien_agent FOREIGN KEY (agent_id) REFERENCES agent_immobilier (utilisateur_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_bien_categorie FOREIGN KEY (categorie_id) REFERENCES categorie (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_bien_prix CHECK (prix > 0),
  CONSTRAINT chk_bien_superficie CHECK (superficie > 0),
  CONSTRAINT chk_bien_chambres CHECK (nb_chambres BETWEEN 0 AND 20),
  CONSTRAINT chk_bien_latitude CHECK (latitude BETWEEN -90 AND 90),
  CONSTRAINT chk_bien_longitude CHECK (longitude BETWEEN -180 AND 180),
  CONSTRAINT chk_bien_cp CHECK (code_postal REGEXP '^[1-9][0-9]{3}$')
) ENGINE=InnoDB;

CREATE TABLE photo (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  bien_id INT UNSIGNED NOT NULL,
  url VARCHAR(255) NOT NULL,
  ordre TINYINT UNSIGNED NOT NULL,
  legende VARCHAR(150) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_photo_bien_ordre (bien_id, ordre),
  CONSTRAINT fk_photo_bien FOREIGN KEY (bien_id) REFERENCES bien (id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT chk_photo_ordre CHECK (ordre >= 1)
) ENGINE=InnoDB;

-- ---------- Favoris (jonction) ----------
CREATE TABLE favori (
  membre_id INT UNSIGNED NOT NULL,
  bien_id INT UNSIGNED NOT NULL,
  date_ajout DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (membre_id, bien_id),
  KEY idx_favori_bien (bien_id),
  CONSTRAINT fk_favori_membre FOREIGN KEY (membre_id) REFERENCES membre (utilisateur_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_favori_bien FOREIGN KEY (bien_id) REFERENCES bien (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------- Messages ----------
CREATE TABLE message (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  membre_id INT UNSIGNED NOT NULL,
  agent_id INT UNSIGNED NOT NULL,
  contenu TEXT NOT NULL,
  envoye_le DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lu TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_message_membre (membre_id),
  KEY idx_message_agent_lu (agent_id, lu),
  CONSTRAINT fk_message_membre FOREIGN KEY (membre_id) REFERENCES membre (utilisateur_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_message_agent FOREIGN KEY (agent_id) REFERENCES agent_immobilier (utilisateur_id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------- Rendez-vous ----------
CREATE TABLE rendez_vous (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  membre_id INT UNSIGNED NOT NULL,
  agent_id INT UNSIGNED NOT NULL,
  bien_id INT UNSIGNED NOT NULL,
  date_heure DATETIME NOT NULL,
  statut ENUM('demande','confirme','annule','honore') NOT NULL DEFAULT 'demande',
  motif VARCHAR(255) NULL,
  PRIMARY KEY (id),
  KEY idx_rdv_membre (membre_id),
  KEY idx_rdv_bien (bien_id),
  KEY idx_rdv_statut (statut),
  KEY idx_rdv_agent_creneau (agent_id, date_heure),
  CONSTRAINT fk_rdv_membre FOREIGN KEY (membre_id) REFERENCES membre (utilisateur_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_rdv_agent FOREIGN KEY (agent_id) REFERENCES agent_immobilier (utilisateur_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_rdv_bien FOREIGN KEY (bien_id) REFERENCES bien (id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------- Paiements ----------
CREATE TABLE paiement (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  rendez_vous_id INT UNSIGNED NOT NULL,
  membre_id INT UNSIGNED NOT NULL,
  stripe_payment_intent_id VARCHAR(120) NOT NULL,
  montant DECIMAL(8,2) NOT NULL,
  statut ENUM('initie','reussi','echoue','rembourse') NOT NULL,
  paye_le DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_paiement_rdv (rendez_vous_id),
  UNIQUE KEY uq_paiement_stripe (stripe_payment_intent_id),
  KEY idx_paiement_membre (membre_id),
  CONSTRAINT fk_paiement_rdv FOREIGN KEY (rendez_vous_id) REFERENCES rendez_vous (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_paiement_membre FOREIGN KEY (membre_id) REFERENCES membre (utilisateur_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_paiement_montant CHECK (montant > 0)
) ENGINE=InnoDB;

-- ---------- Blog ----------
CREATE TABLE article (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  administrateur_id INT UNSIGNED NOT NULL,
  categorie_article_id INT UNSIGNED NOT NULL,
  titre VARCHAR(150) NOT NULL,
  contenu TEXT NOT NULL,
  statut ENUM('brouillon','publie','archive') NOT NULL DEFAULT 'brouillon',
  publie_le DATE NULL,
  PRIMARY KEY (id),
  KEY idx_article_admin (administrateur_id),
  KEY idx_article_categorie (categorie_article_id),
  KEY idx_article_statut (statut),
  CONSTRAINT fk_article_admin FOREIGN KEY (administrateur_id) REFERENCES administrateur (utilisateur_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_article_categorie FOREIGN KEY (categorie_article_id) REFERENCES categorie_article (id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------- Journal d'audit ----------
CREATE TABLE journal_audit (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  utilisateur_id INT UNSIGNED NOT NULL,
  action VARCHAR(60) NOT NULL,
  entite VARCHAR(60) NOT NULL,
  horodatage DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip VARCHAR(45) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_journal_utilisateur (utilisateur_id),
  KEY idx_journal_horodatage (horodatage),
  CONSTRAINT fk_journal_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------- Traductions ----------
CREATE TABLE traduction (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  langue_id INT UNSIGNED NOT NULL,
  cle VARCHAR(120) NOT NULL,
  valeur TEXT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_traduction_langue_cle (langue_id, cle),
  CONSTRAINT fk_traduction_langue FOREIGN KEY (langue_id) REFERENCES langue (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------- Clés API ----------
CREATE TABLE cle_api (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  administrateur_id INT UNSIGNED NOT NULL,
  cle VARCHAR(64) NOT NULL,
  cree_le DATE NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_cle_api_cle (cle),
  KEY idx_cle_api_admin (administrateur_id),
  CONSTRAINT fk_cle_api_admin FOREIGN KEY (administrateur_id) REFERENCES administrateur (utilisateur_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
