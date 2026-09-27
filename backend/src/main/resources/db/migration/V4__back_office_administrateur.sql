-- ============================================================
-- ImmoConnect — Back-office de l'administrateur (cas A1 et A7)
-- ============================================================

-- A1 — Activer ou désactiver un compte : un compte désactivé ne peut plus se connecter.
-- Rien n'est supprimé, l'historique (annonces, rendez-vous, paiements) reste intact.
ALTER TABLE utilisateur
  ADD COLUMN actif TINYINT(1) NOT NULL DEFAULT 1 AFTER role;

-- A7 — Clés API : à qui la clé a été délivrée, et quand elle a servi pour la dernière fois.
-- La colonne cle contient désormais l'empreinte SHA-256 de la clé (64 caractères hexadécimaux),
-- jamais la clé elle-même : une fuite de la base ne livre aucune clé utilisable.
ALTER TABLE cle_api
  ADD COLUMN libelle VARCHAR(80) NULL AFTER cle,
  ADD COLUMN derniere_utilisation DATETIME NULL AFTER active;

-- Données de test : les valeurs existantes sont conservées telles quelles et tenues pour des
-- empreintes. Aucune clé ne leur correspond : ces lignes illustrent la liste du back-office,
-- et aucune clé valide ne figure dans le dépôt public.
UPDATE cle_api SET libelle = ELT(id, 'Observatoire des loyers de Bruxelles', 'Comparateur ImmoPrix',
                                 'Mémoire de fin d''études — ULB', 'Tableau de bord interne de l''agence',
                                 'Portail partenaire (ancien contrat)', 'Application mobile — prototype')
 WHERE id <= 6;

-- Données de test : une clé ne peut pas avoir été créée après la génération du jeu d'essai.
UPDATE cle_api
   SET cree_le = DATE_SUB(cree_le, INTERVAL 1 YEAR)
 WHERE cree_le > '2026-08-21';

-- Données de test : le jeu d'essai contenait des événements datés dans le futur (inscription,
-- publication, favori, paiement, trace d'audit). Un événement enregistré a forcément déjà eu lieu :
-- ces dates sont ramenées un an en arrière. Les rendez-vous ne sont pas concernés, une visite
-- se prévoit à l'avance.
UPDATE utilisateur   SET date_inscription = DATE_SUB(date_inscription, INTERVAL 1 YEAR) WHERE date_inscription > '2026-09-27';
UPDATE bien          SET publie_le        = DATE_SUB(publie_le, INTERVAL 1 YEAR)        WHERE publie_le > '2026-09-27';
UPDATE article       SET publie_le        = DATE_SUB(publie_le, INTERVAL 1 YEAR)        WHERE publie_le > '2026-09-27';
UPDATE favori        SET date_ajout       = DATE_SUB(date_ajout, INTERVAL 1 YEAR)       WHERE date_ajout > '2026-09-27 23:59:59';
UPDATE paiement      SET paye_le          = DATE_SUB(paye_le, INTERVAL 1 YEAR)          WHERE paye_le > '2026-09-27 23:59:59';
UPDATE journal_audit SET horodatage       = DATE_SUB(horodatage, INTERVAL 1 YEAR)       WHERE horodatage > '2026-09-27 23:59:59';
