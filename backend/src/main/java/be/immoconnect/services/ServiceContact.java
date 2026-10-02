package be.immoconnect.services;

import be.immoconnect.dto.DemandeContactResume;
import be.immoconnect.dto.RequeteContact;
import be.immoconnect.entities.Administrateur;
import be.immoconnect.entities.DemandeContact;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.exceptions.TropDeTentativesException;
import be.immoconnect.notification.EvenementContact;
import be.immoconnect.repositories.DemandeContactRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Formulaire de contact : la demande est enregistrée, transmise par e-mail à l'agence et suivie dans le
 * back-office. Le formulaire est public ; deux parades contre l'abus : un champ piège pour les robots et un
 * nombre de messages par heure et par adresse IP.
 */
@Service
public class ServiceContact {

    private static final int ENTREES_MAX = 10_000;

    private record Fenetre(long heure, int envois) {
    }

    private final ConcurrentHashMap<String, Fenetre> parAdresseIp = new ConcurrentHashMap<>();
    private final DemandeContactRepository demandes;
    private final AccesAdministrateur acces;
    private final ServiceAudit audit;
    private final ApplicationEventPublisher evenements;
    private final Clock horloge;
    private final int messagesParHeure;

    public ServiceContact(DemandeContactRepository demandes, AccesAdministrateur acces, ServiceAudit audit,
                          ApplicationEventPublisher evenements, Clock horloge,
                          @Value("${immoconnect.contact.messages-par-heure:5}") int messagesParHeure) {
        this.demandes = demandes;
        this.acces = acces;
        this.audit = audit;
        this.evenements = evenements;
        this.horloge = horloge;
        this.messagesParHeure = messagesParHeure;
    }

    @Transactional
    public void envoyer(RequeteContact requete, String ip) {
        // Champ piège rempli : un robot. La demande est ignorée, sans erreur qui l'aiderait à s'adapter.
        if (requete.site() != null && !requete.site().isBlank()) {
            return;
        }
        limiter(ip);
        String telephone = requete.telephone() == null || requete.telephone().isBlank() ? null : requete.telephone().trim();
        DemandeContact demande = demandes.save(new DemandeContact(requete.nom().trim(), requete.email().trim(), telephone,
                requete.sujet().trim(), requete.message().trim(), LocalDateTime.now(horloge), ip == null ? "inconnue" : ip));
        evenements.publishEvent(new EvenementContact(demande.getId()));
    }

    @Transactional(readOnly = true)
    public Page<DemandeContactResume> lister(Integer administrateurId, boolean enAttenteSeulement, Pageable pagination) {
        acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        return (enAttenteSeulement ? demandes.findByTraiteLeIsNullOrderByCreeLeDescIdDesc(pagination)
                : demandes.findAllByOrderByCreeLeDescIdDesc(pagination)).map(DemandeContactResume::depuis);
    }

    /** L'administrateur a répondu à la personne : la demande est classée, à son nom. */
    @Transactional
    public DemandeContactResume traiter(Integer administrateurId, Integer id, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        DemandeContact demande = demandes.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Demande de contact", id));
        if (!demande.estTraitee()) {
            demande.traiter(administrateur, LocalDateTime.now(horloge));
            audit.enregistrer(administrateur, "traitement_contact", "demande_contact#" + id, ip);
        }
        return DemandeContactResume.depuis(demande);
    }

    private void limiter(String ip) {
        long heure = horloge.millis() / 3_600_000;
        if (parAdresseIp.size() > ENTREES_MAX) {
            parAdresseIp.values().removeIf(f -> f.heure() < heure);
        }
        Fenetre fenetre = parAdresseIp.merge(ip == null ? "inconnue" : ip, new Fenetre(heure, 1),
                (ancienne, nouvelle) -> ancienne.heure() == heure ? new Fenetre(heure, ancienne.envois() + 1) : nouvelle);
        if (fenetre.envois() > messagesParHeure) {
            throw new TropDeTentativesException(3600 - (horloge.millis() / 1000) % 3600);
        }
    }
}
