package be.immoconnect.services;

import be.immoconnect.dto.AlerteResume;
import be.immoconnect.entities.Administrateur;
import be.immoconnect.entities.AlerteSecurite;
import be.immoconnect.notification.ExpediteurCourriel;
import be.immoconnect.repositories.AdministrateurRepository;
import be.immoconnect.repositories.AlerteSecuriteRepository;
import be.immoconnect.security.DetectionIntrusion;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suite donnée à une alerte de la détection d'intrusion (chapitre 9.6) : elle est enregistrée, écrite au
 * journal du serveur sur une ligne que fail2ban sait lire, et envoyée aux super-administrateurs, qui
 * appliquent la procédure — vérifier, bloquer ou révoquer, évaluer l'impact, documenter.
 */
@Service
public class ServiceAlertesSecurite {

    /** Journal dédié : ses lignes « ALERTE_SECURITE … ip=… » alimentent le bannissement au niveau du serveur. */
    private static final Logger journal = LoggerFactory.getLogger("be.immoconnect.securite");
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AlerteSecuriteRepository alertes;
    private final AdministrateurRepository administrateurs;
    private final AccesAdministrateur acces;
    private final ExpediteurCourriel expediteur;
    private final Clock horloge;

    public ServiceAlertesSecurite(AlerteSecuriteRepository alertes, AdministrateurRepository administrateurs,
                                  AccesAdministrateur acces, ExpediteurCourriel expediteur, Clock horloge) {
        this.alertes = alertes;
        this.administrateurs = administrateurs;
        this.acces = acces;
        this.expediteur = expediteur;
        this.horloge = horloge;
    }

    /** Hors du fil de la requête : l'attaquant n'attend pas, et un courriel lent ne ralentit personne. */
    @Async
    @EventListener
    @Transactional
    public void traiter(DetectionIntrusion.Alerte alerte) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        AlerteSecurite enregistree = alertes.save(new AlerteSecurite(alerte.type(), alerte.ip(), alerte.detail(), maintenant));
        journal.warn("ALERTE_SECURITE type={} ip={} detail=\"{}\"", alerte.type(), alerte.ip(), alerte.detail());
        for (Administrateur administrateur : administrateurs.findAll()) {
            if (administrateur.getNiveauAcces() >= AccesAdministrateur.SUPER_ADMINISTRATEUR && administrateur.isActif()) {
                expediteur.envoyer(administrateur.getEmail(), Locale.of(administrateur.getLangue().getCode()), "securite.alerte",
                        new Object[] {administrateur.getPrenom(), alerte.type().name(), alerte.ip(), alerte.detail(), maintenant.format(HORODATAGE)},
                        "alerte " + enregistree.getId());
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<AlerteResume> lister(Integer administrateurId, Pageable pagination) {
        acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        return alertes.findAllByOrderByCreeLeDescIdDesc(pagination).map(AlerteResume::depuis);
    }
}
