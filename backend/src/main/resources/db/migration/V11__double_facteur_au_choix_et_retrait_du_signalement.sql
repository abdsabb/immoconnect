-- Connexion en deux étapes : elle n'est plus imposée à aucun rôle. Chacun l'active dans son profil ;
-- les comptes existants repartent donc sans, agents et administrateurs compris.
UPDATE utilisateur SET double_facteur = 0;

-- Le signalement de contenus est retiré : l'agence est une structure privée, sans contenu publié par des tiers.
-- Les messages restent des échanges privés entre un client et son agent, que la direction peut consulter.
DROP TABLE signalement;
