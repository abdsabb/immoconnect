package be.immoconnect.notification;

import be.immoconnect.entities.RendezVous;
import be.immoconnect.entities.StatutPaiement;
import be.immoconnect.entities.Utilisateur;
import be.immoconnect.repositories.RendezVousRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    private static final Logger journal = LoggerFactory.getLogger(NotificateurCourriel.class);
    private static final int TENTATIVES = 3;

    private final RendezVousRepository rendezVous;
    private final JavaMailSender messagerie;
    private final MessageSource textes;
    private final String expediteur;
    private final String urlSite;
    private final Duration delaiEntreTentatives;

    public NotificateurCourriel(RendezVousRepository rendezVous, JavaMailSender messagerie,
                                @Value("${immoconnect.courriel.expediteur}") String expediteur,
                                @Value("${immoconnect.courriel.url-site}") String urlSite,
                                @Value("${immoconnect.courriel.delai-entre-tentatives}") Duration delaiEntreTentatives) {
        this.rendezVous = rendezVous;
        this.messagerie = messagerie;
        this.textes = textesDesCourriels();
        this.expediteur = expediteur;
        this.urlSite = urlSite;
        this.delaiEntreTentatives = delaiEntreTentatives;
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(expediteur);
        message.setTo(destinataire.getEmail());
        message.setSubject(textes.getMessage(modele + ".sujet", arguments, langue));
        message.setText(textes.getMessage(modele + ".corps", arguments, langue)
                + "\n\n" + textes.getMessage("signature", null, langue));
        envoyerAvecReprise(message, modele, rdv.getId());
    }

    private void envoyerAvecReprise(SimpleMailMessage message, String modele, Integer rendezVousId) {
        for (int tentative = 1; tentative <= TENTATIVES; tentative++) {
            try {
                messagerie.send(message);
                return;
            } catch (MailException e) {
                journal.warn("E-mail « {} » du rendez-vous {} non envoyé (tentative {}/{}) : {}",
                        modele, rendezVousId, tentative, TENTATIVES, e.getMessage());
            }
            if (tentative < TENTATIVES && !patienter()) {
                return;
            }
        }
        journal.error("E-mail « {} » du rendez-vous {} abandonné après {} tentatives ; le rendez-vous reste valide",
                modele, rendezVousId, TENTATIVES);
    }

    private boolean patienter() {
        try {
            Thread.sleep(delaiEntreTentatives);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Textes en français, néerlandais et anglais : src/main/resources/courriels/messages_xx.properties. */
    private static MessageSource textesDesCourriels() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("courriels/messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setFallbackToSystemLocale(false);
        return source;
    }

    private static boolean estRembourse(RendezVous rdv) {
        return rdv.getPaiement() != null && rdv.getPaiement().getStatut() == StatutPaiement.rembourse;
    }
}
