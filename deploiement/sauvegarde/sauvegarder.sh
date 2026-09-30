#!/bin/bash
# Sauvegardes d'ImmoConnect (chapitre 6 et 9.6 du rapport) — tourne dans le conteneur « sauvegarde ».
#   - chaque nuit : export de la base (mysqldump), compressé, chiffré si SAUVEGARDE_PASSPHRASE est fournie ;
#   - chaque dimanche : archive des photos des biens ;
#   - rétention : 14 sauvegardes quotidiennes de la base, 8 archives hebdomadaires des photos.
# Une sauvegarde lancée à la main : docker compose -f docker-compose.prod.yml exec sauvegarde bash /sauvegarder.sh maintenant
set -euo pipefail

DOSSIER=/sauvegardes
HEURE="${SAUVEGARDE_HEURE:-03:15}"
JOURS_BASE="${SAUVEGARDE_RETENTION_BASE:-14}"
SEMAINES_PHOTOS="${SAUVEGARDE_RETENTION_PHOTOS:-8}"

journal() { echo "$(date '+%Y-%m-%d %H:%M:%S') sauvegarde : $*"; }

# Chiffrement AES-256 (clé dérivée par PBKDF2) quand une phrase de passe est fournie
chiffrer() {
  if [ -n "${SAUVEGARDE_PASSPHRASE:-}" ]; then
    openssl enc -aes-256-cbc -pbkdf2 -iter 200000 -salt -pass env:SAUVEGARDE_PASSPHRASE
  else
    cat
  fi
}
suffixe() { if [ -n "${SAUVEGARDE_PASSPHRASE:-}" ]; then echo ".enc"; fi; }

sauvegarder_base() {
  local fichier="$DOSSIER/base/immoconnect-$(date '+%Y-%m-%d_%H%M').sql.gz$(suffixe)"
  mkdir -p "$DOSSIER/base"
  # --single-transaction : image cohérente de la base sans bloquer le site
  MYSQL_PWD="$DB_PASSWORD" mysqldump --host=db --user=immo --single-transaction --routines --no-tablespaces \
      --default-character-set=utf8mb4 immoconnect | gzip -9 | chiffrer > "$fichier.partiel"
  mv "$fichier.partiel" "$fichier"
  journal "base enregistrée dans $(basename "$fichier") ($(du -h "$fichier" | cut -f1))"
  find "$DOSSIER/base" -name 'immoconnect-*.sql.gz*' -mtime +"$JOURS_BASE" -delete
}

sauvegarder_photos() {
  local fichier="$DOSSIER/photos/photos-$(date '+%Y-%m-%d').tar.gz$(suffixe)"
  mkdir -p "$DOSSIER/photos"
  tar -C /photos -czf - . | chiffrer > "$fichier.partiel"
  mv "$fichier.partiel" "$fichier"
  journal "photos enregistrées dans $(basename "$fichier") ($(du -h "$fichier" | cut -f1))"
  find "$DOSSIER/photos" -name 'photos-*.tar.gz*' -mtime +"$((SEMAINES_PHOTOS * 7))" -delete
}

if [ "${1:-}" = "maintenant" ]; then
  sauvegarder_base
  sauvegarder_photos
  exit 0
fi

[ -n "${SAUVEGARDE_PASSPHRASE:-}" ] || journal "ATTENTION : SAUVEGARDE_PASSPHRASE est vide, les sauvegardes ne sont pas chiffrées"
journal "démarrage — base chaque nuit à $HEURE, photos le dimanche ; rétention $JOURS_BASE jours / $SEMAINES_PHOTOS semaines"

# Première sauvegarde au démarrage s'il n'en existe aucune : le volume n'est jamais vide
if ! ls "$DOSSIER"/base/immoconnect-*.sql.gz* >/dev/null 2>&1; then
  sauvegarder_base || journal "ÉCHEC de la sauvegarde initiale de la base"
  sauvegarder_photos || journal "ÉCHEC de la sauvegarde initiale des photos"
fi

while true; do
  maintenant=$(date '+%s')
  prochaine=$(date -d "today $HEURE" '+%s')
  [ "$prochaine" -gt "$maintenant" ] || prochaine=$(date -d "tomorrow $HEURE" '+%s')
  sleep $((prochaine - maintenant))
  sauvegarder_base || journal "ÉCHEC de la sauvegarde de la base"
  if [ "$(date '+%u')" = "7" ]; then
    sauvegarder_photos || journal "ÉCHEC de la sauvegarde des photos"
  fi
done
