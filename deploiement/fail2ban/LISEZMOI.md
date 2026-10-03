# Bannissement au niveau du serveur (fail2ban) et pare-feu

L'application détecte elle-même les comportements anormaux (rafale d'échecs de connexion, énumération
d'identifiants) et bannit l'adresse de la **connexion** pendant quinze minutes. fail2ban prolonge ce
bannissement au niveau du **serveur** : il lit les alertes dans le journal du backend et ferme les ports
web à l'adresse pendant une heure (une journée en cas de récidive).

À faire une fois sur le serveur, en `root` :

```bash
apt-get update && apt-get install -y fail2ban ufw

# Pare-feu : seuls SSH et le web sont ouverts
ufw default deny incoming && ufw default allow outgoing
ufw allow 22/tcp && ufw allow 80/tcp && ufw allow 443/tcp
ufw --force enable

# Filtre et prison ImmoConnect
cp /opt/immoconnect/deploiement/fail2ban/filter.d/immoconnect.conf /etc/fail2ban/filter.d/
cp /opt/immoconnect/deploiement/fail2ban/jail.d/immoconnect.conf /etc/fail2ban/jail.d/
systemctl enable --now fail2ban && systemctl restart fail2ban
```

Vérifier :

```bash
fail2ban-client status immoconnect          # prison active, adresses bannies

# Tester le filtre sur le journal du backend (un seul fichier à la fois) ; « 0 matched » tant qu'aucune alerte n'a eu lieu
cd /opt/immoconnect
fail2ban-regex "$(docker inspect --format '{{.LogPath}}' "$(docker compose -f docker-compose.prod.yml ps -q backend)")" \
  /etc/fail2ban/filter.d/immoconnect.conf
```

Lever un bannissement, en remplaçant l'adresse d'exemple par celle qui figure dans la liste des adresses bannies :

```bash
fail2ban-client set immoconnect unbanip 203.0.113.9
```

fail2ban surveille aussi SSH par défaut (prison `sshd`) : les tentatives de connexion au serveur lui-même
sont bannies de la même façon.
