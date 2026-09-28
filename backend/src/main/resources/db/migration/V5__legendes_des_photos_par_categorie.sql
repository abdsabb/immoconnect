-- ImmoConnect — V5 : légendes des photos de test cohérentes avec l'annonce.
-- Les légendes des données de test ont été tirées au hasard. Deux corrections :
--   1. un terrain, un garage ou une surface commerciale n'a ni cuisine ni chambre ;
--   2. la photo de couverture d'un logement montre sa façade ou son séjour, pas sa salle de bains.
-- Les photos téléversées par les agents (nom de fichier aléatoire) ne sont pas concernées.

UPDATE photo p JOIN bien b ON b.id = p.bien_id
   SET p.legende = IF(MOD(b.id, 3) = 0, 'Séjour', 'Façade')
 WHERE p.ordre = 1
   AND b.categorie_id NOT IN (7, 9, 10)
   AND (p.legende IS NULL OR p.legende NOT IN ('Façade', 'Séjour'))
   AND p.url LIKE '/storage/biens/%/photo-%.jpg';

UPDATE photo p JOIN bien b ON b.id = p.bien_id
   SET p.legende = 'Terrain'
 WHERE b.categorie_id = 7 AND p.url LIKE '/storage/biens/%/photo-%.jpg';

UPDATE photo p JOIN bien b ON b.id = p.bien_id
   SET p.legende = 'Garage'
 WHERE b.categorie_id = 9 AND p.url LIKE '/storage/biens/%/photo-%.jpg';

UPDATE photo p JOIN bien b ON b.id = p.bien_id
   SET p.legende = 'Espace commercial'
 WHERE b.categorie_id = 10 AND p.url LIKE '/storage/biens/%/photo-%.jpg';
