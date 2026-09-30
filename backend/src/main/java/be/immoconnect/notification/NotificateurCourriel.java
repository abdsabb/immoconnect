package be.immoconnect.notification;

import be.immoconnect.entities.RendezVous;
import be.immoconnect.entities.StatutPaiement;
import be.immoconnect.entities.Utilisateur;
import be.immoconnect.repositories.RendezVousRepository;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observateur des rendez-vous : prévient par e-mail le membre et l'agent, chacun dans sa langue.
 * <p>
 * Il n'agit qu'après la validation de la transaction (AFTER_COMMIT) : aucun e-mail ne part pour un
 * rendez-vous finalement refusé. Cas d'erreur E2 : si l'envoi échoue, le rendez-vous reste valide,
 * l'incident est journalisé et l'envoi retenté.
 */
@Component
public class NotificateurCourriel {

    private final RendezVousRepository rendezVous;
    private final ExpediteurCourriel expediteur;
    private final String urlSite;

    public NotificateurCourriel(RendezVousRepository rendezVous, ExpediteurCourriel expediteur,
                                @Value("${immoconnect.courriel.url-site}") String urlSite) {
        this.rendezVous = rendezVous;
        this.expediteur = expediteur;
        this.urlSite = urlSite;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void notifier(EvenementRendezVous evenement) {
        rendezVous.findWithDetailsById(evenement.rendezVousId()).ifPresent(rdv -> {
            switch (evenement.type()) {
                case demande -> envoyer(rdv.getAgent(), "demande.agent", rdv);
                case confirme_par_paiement -> {
                    envoyer(rdv.getMembre(), "confirme.membre", rdv);
                    envoyer(rdv.getAgent(), "paye.agent", rdv);
                }
                case confirme_par_agent -> envoyer(rdv.getMembre(), "confirme.membre", rdv);
                case annule_par_membre -> envoyer(rdv.getAgent(), "annule.agent", rdv);
                case annule_par_agent -> envoyer(rdv.getMembre(), estRembourse(rdv) ? "annule.membre.rembourse" : "annule.membre", rdv);
            }
        });
    }

    private void envoyer(Utilisateur destinataire, String modele, RendezVous rdv) {
        Locale langue = Locale.of(destinataire.getLangue().getCode());
        Object[] arguments = {
                destinataire.getPrenom(),                                                      // {0}
                rdv.getBien().getTitre(),                                                      // {1}
                rdv.getDateHeure().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(langue)),   // {2}
                rdv.getDateHeure().format(DateTimeFormatter.ofPattern("HH:mm")),               // {3}
                rdv.getBien().getAdresse() + ", " + rdv.getBien().getCodePostal() + " " + rdv.getBien().getVille(), // {4}
                rdv.getAgent().getNomComplet(),                                                // {5}
                rdv.getAgent().getTelephonePro(),                                              // {6}
                rdv.getMembre().getNomComplet(),                                               // {7}
                rdv.getMotif() == null ? "—" : rdv.getMotif(),                                 // {8}
                urlSite + "/rendez-vous"                                                       // {9}
        };
        expediteur.envoyer(destinataire.getEmail(), langue, modele, arguments, "rendez-vous " + rdv.getId());
    }

    private static boolean estRembourse(RendezVous rdv) {
        return rdv.getPaiement() != null && rdv.getPaiement().getStatut() == StatutPaiement.rembourse;
    }
}
