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
fail2ban-regex /var/lib/docker/containers/*/*-json.log /etc/fail2ban/filter.d/immoconnect.conf
fail2ban-client set immoconnect unbanip 203.0.113.9   # lever un bannissement
```

fail2ban surveille aussi SSH par défaut (prison `sshd`) : les tentatives de connexion au serveur lui-même
sont bannies de la même façon.
