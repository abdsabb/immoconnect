-- ============================================================
-- ImmoConnect — Messagerie : réponses des agents (cas AG4)
-- ============================================================
-- La table message reliait un membre à un agent sans dire lequel des deux avait écrit :
-- seul le sens membre → agent était représentable. La colonne expediteur lève l'ambiguïté.
-- Le couple (membre_id, agent_id) identifie toujours la conversation ; « lu » signifie
-- désormais « lu par son destinataire », quel qu'il soit.

ALTER TABLE message
  ADD COLUMN expediteur ENUM('membre','agent') NOT NULL DEFAULT 'membre' AFTER agent_id,
  ADD KEY idx_message_conversation (membre_id, agent_id, envoye_le);

-- Données de test : aucun message ne peut avoir été envoyé après la génération du jeu d'essai.
UPDATE message
   SET envoye_le = DATE_SUB(envoye_le, INTERVAL 1 YEAR)
 WHERE envoye_le > '2026-08-21 00:00:00';

-- Données de test : chaque message déjà lu par l'agent reçoit sa réponse, quelques heures plus tard.
INSERT INTO message (membre_id, agent_id, expediteur, contenu, envoye_le, lu)
SELECT m.membre_id, m.agent_id, 'agent',
       ELT(1 + (m.id MOD 5),
           'Bonjour, merci pour votre message. Le plus simple est d''en parler sur place : vous pouvez réserver un créneau de visite depuis la fiche du bien.',
           'Bonjour, je me renseigne auprès du propriétaire et je reviens vers vous d''ici demain avec une réponse précise.',
           'Bonjour, oui, tout est repris dans le dossier du bien. Je vous le remets lors de la visite, avec le certificat PEB.',
           'Bonjour, merci de votre intérêt. Le bien est toujours disponible ; je peux vous le faire visiter cette semaine, y compris en soirée.',
           'Bonjour, bonne question : je vous envoie le détail par e-mail aujourd''hui. N''hésitez pas à m''appeler si vous préférez en discuter.'),
       DATE_ADD(m.envoye_le, INTERVAL 2 + (m.id MOD 20) HOUR),
       IF(m.id MOD 6 = 0, 0, 1)
  FROM message m
 WHERE m.lu = 1;
