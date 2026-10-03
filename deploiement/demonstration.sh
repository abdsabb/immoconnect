#!/bin/sh
# Requêtes de démonstration, à lancer dans le conteneur de la base (voir docs ou README) :
#
#   docker compose -f docker-compose.prod.yml exec -T db sh -s -- tables     < deploiement/demonstration.sh
#   docker compose -f docker-compose.prod.yml exec -T db sh -s -- membre 112 < deploiement/demonstration.sh
#
# « tables » : nombre de lignes de chaque table (données de test en quantité suffisante).
# « membre N » : ce que la base garde du compte N — à lancer avant puis après une désinscription, pour montrer
# l'anonymisation (« soft delete », règle RA11) : l'identité disparaît, les rendez-vous, paiements et traces restent.
# Lecture seule : aucune de ces requêtes ne modifie la base.

q() {
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 immoconnect "$@" 2>/dev/null
}

case "$1" in
  tables)
    for t in $(q -N -e "SHOW TABLES"); do
      printf '%-24s %s\n' "$t" "$(q -N -e "SELECT COUNT(*) FROM $t")"
    done
    ;;
  membre)
    case "$2" in
      ''|*[!0-9]*) echo "Usage : membre <identifiant numérique>" >&2; exit 2 ;;
    esac
    echo "-- Compte"
    q -t -e "SELECT u.id, u.nom, u.prenom, u.email, m.telephone, LEFT(u.mot_de_passe, 14) AS mot_de_passe_hache
               FROM utilisateur u JOIN membre m ON m.utilisateur_id = u.id WHERE u.id = $2"
    echo "-- Favoris et messages"
    q -t -e "SELECT (SELECT COUNT(*) FROM favori WHERE membre_id = $2) AS favoris,
                    (SELECT COUNT(*) FROM message WHERE membre_id = $2) AS messages,
                    (SELECT COUNT(*) FROM message WHERE membre_id = $2 AND contenu LIKE '[Contenu supprimé%') AS messages_vides"
    echo "-- Rendez-vous et paiements (conservés)"
    q -t -e "SELECT r.id, r.statut, r.date_heure, p.montant, p.statut AS paiement
               FROM rendez_vous r LEFT JOIN paiement p ON p.rendez_vous_id = r.id WHERE r.membre_id = $2 ORDER BY r.id"
    echo "-- Journal d'audit (conservé)"
    q -t -e "SELECT action, entite, horodatage, ip FROM journal_audit WHERE utilisateur_id = $2 ORDER BY id"
    ;;
  *)
    echo "Usage : tables | membre <identifiant>" >&2
    exit 2
    ;;
esac
