#!/bin/bash
# Restauration d'une sauvegarde de la base — tourne dans le conteneur « sauvegarde ».
# « Une sauvegarde jamais restaurée n'est pas une sauvegarde » : ce script sert au test trimestriel comme à la reprise.
#
#   Lister les sauvegardes :
#     docker compose -f docker-compose.prod.yml exec sauvegarde bash /restaurer.sh
#   Vérifier qu'une sauvegarde se relit, dans une base jetable (le site n'est pas touché) :
#     docker compose -f docker-compose.prod.yml exec sauvegarde bash /restaurer.sh verifier immoconnect-2026-10-01_0315.sql.gz.enc
#   Restaurer pour de bon (REMPLACE les données du site — arrêter le backend avant) :
#     docker compose -f docker-compose.prod.yml stop backend
#     docker compose -f docker-compose.prod.yml exec sauvegarde bash /restaurer.sh restaurer immoconnect-2026-10-01_0315.sql.gz.enc
#     docker compose -f docker-compose.prod.yml start backend
set -euo pipefail

DOSSIER=/sauvegardes/base
action="${1:-lister}"
fichier="${2:-}"

dechiffrer() {
  case "$1" in
    *.enc) openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass env:SAUVEGARDE_PASSPHRASE -in "$1" ;;
    *) cat "$1" ;;
  esac
}
racine() { MYSQL_PWD="$DB_ROOT_PASSWORD" mysql --host=db --user=root --default-character-set=utf8mb4 "$@"; }

if [ "$action" = "lister" ]; then
  ls -lh "$DOSSIER" 2>/dev/null || echo "aucune sauvegarde"
  exit 0
fi
[ -n "$fichier" ] && [ -f "$DOSSIER/$fichier" ] || { echo "sauvegarde introuvable : $fichier"; exit 1; }

case "$action" in
  verifier)
    # Base jetable : la sauvegarde y est relue entièrement, puis la base est supprimée
    racine -e "DROP DATABASE IF EXISTS immoconnect_verification; CREATE DATABASE immoconnect_verification CHARACTER SET utf8mb4;"
    dechiffrer "$DOSSIER/$fichier" | gunzip | racine immoconnect_verification
    racine -N -e "SELECT CONCAT('tables : ', COUNT(*)) FROM information_schema.tables WHERE table_schema = 'immoconnect_verification';
                  SELECT CONCAT('biens : ', COUNT(*)) FROM immoconnect_verification.bien;
                  SELECT CONCAT('utilisateurs : ', COUNT(*)) FROM immoconnect_verification.utilisateur;
                  SELECT CONCAT('migrations : ', MAX(CAST(version AS UNSIGNED))) FROM immoconnect_verification.flyway_schema_history;"
    racine -e "DROP DATABASE immoconnect_verification;"
    echo "sauvegarde relue avec succès : $fichier"
    ;;
  restaurer)
    racine -e "DROP DATABASE IF EXISTS immoconnect; CREATE DATABASE immoconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    dechiffrer "$DOSSIER/$fichier" | gunzip | racine immoconnect
    echo "base restaurée depuis $fichier — redémarrer le backend"
    ;;
  *)
    echo "usage : restaurer.sh [lister | verifier <fichier> | restaurer <fichier>]"; exit 1 ;;
esac
