package be.immoconnect.notification;

import be.immoconnect.repositories.DemandeContactRepository;
import be.immoconnect.services.ServiceParametres;
import java.util.Locale;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Transmet chaque demande de contact à l'adresse e-mail de l'agence (paramètre du site, cas A5), une fois la
 * demande enregistrée et hors du fil de la requête : un serveur de courriel lent ne fait pas attendre le visiteur.
 */
@Component
public class CourrielsContact {

    private final DemandeContactRepository demandes;
    private final ServiceParametres parametres;
    private final ExpediteurCourriel expediteur;

    public CourrielsContact(DemandeContactRepository demandes, ServiceParametres parametres, ExpediteurCourriel expediteur) {
        this.demandes = demandes;
        this.parametres = parametres;
        this.expediteur = expediteur;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void transmettre(EvenementContact evenement) {
        demandes.findById(evenement.demandeId()).ifPresent(d -> expediteur.envoyer(parametres.site().email(), Locale.FRENCH, "contact.agence",
                new Object[] {d.getNom(), d.getEmail(), d.getTelephone() == null ? "—" : d.getTelephone(), d.getSujet(), d.getMessage()},
                "demande de contact " + d.getId()));
    }
}
