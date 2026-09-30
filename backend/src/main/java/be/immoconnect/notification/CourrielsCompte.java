package be.immoconnect.notification;

import be.immoconnect.repositories.UtilisateurRepository;
import be.immoconnect.services.ServiceJetonsUniques;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observateur des comptes : lien d'activation, lien de réinitialisation, code de connexion et
 * confirmation d'un changement de mot de passe, chacun dans la langue de l'utilisateur.
 * N'agit qu'après validation de la transaction : aucun lien ne part pour un compte finalement refusé.
 */
@Component
public class CourrielsCompte {

    private final UtilisateurRepository utilisateurs;
    private final ExpediteurCourriel expediteur;
    private final String urlSite;

    public CourrielsCompte(UtilisateurRepository utilisateurs, ExpediteurCourriel expediteur,
                           @Value("${immoconnect.courriel.url-site}") String urlSite) {
        this.utilisateurs = utilisateurs;
        this.expediteur = expediteur;
        this.urlSite = urlSite.replaceAll("/+$", "");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void notifier(EvenementCompte evenement) {
        utilisateurs.findById(evenement.utilisateurId()).ifPresent(utilisateur -> {
            Locale langue = Locale.of(utilisateur.getLangue().getCode());
            String reference = "utilisateur " + utilisateur.getId();
            Object[] arguments = switch (evenement.type()) {
                case activation -> new Object[] {utilisateur.getPrenom(), urlSite + "/activation?jeton=" + evenement.secret(),
                        ServiceJetonsUniques.DUREE_ACTIVATION.toHours()};
                case reinitialisation -> new Object[] {utilisateur.getPrenom(), urlSite + "/reinitialisation?jeton=" + evenement.secret(),
                        ServiceJetonsUniques.DUREE_REINITIALISATION.toMinutes()};
                case mot_de_passe_modifie -> new Object[] {utilisateur.getPrenom(), urlSite + "/mot-de-passe-oublie"};
                case code_connexion -> new Object[] {utilisateur.getPrenom(), evenement.secret(),
                        ServiceJetonsUniques.DUREE_DEFI.toMinutes()};
            };
            expediteur.envoyer(utilisateur.getEmail(), langue, "compte." + evenement.type().name(), arguments, reference);
        });
    }
}
