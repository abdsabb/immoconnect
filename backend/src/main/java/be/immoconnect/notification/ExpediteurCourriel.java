package be.immoconnect.notification;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envoi d'un e-mail à partir d'un modèle traduit (src/main/resources/courriels/messages_xx.properties).
 * Un envoi qui échoue est retenté ; s'il échoue encore, l'incident est journalisé et l'opération
 * métier qui l'a déclenché reste valide (cas d'erreur E2).
 */
@Component
public class ExpediteurCourriel {

    private static final Logger journal = LoggerFactory.getLogger(ExpediteurCourriel.class);
    static final int TENTATIVES = 3;

    private final JavaMailSender messagerie;
    private final MessageSource textes;
    private final String expediteur;
    private final Duration delaiEntreTentatives;

    public ExpediteurCourriel(JavaMailSender messagerie,
                              @Value("${immoconnect.courriel.expediteur}") String expediteur,
                              @Value("${immoconnect.courriel.delai-entre-tentatives}") Duration delaiEntreTentatives) {
        this.messagerie = messagerie;
        this.textes = textesDesCourriels();
        this.expediteur = expediteur;
        this.delaiEntreTentatives = delaiEntreTentatives;
    }

    /**
     * @param modele    préfixe des clés « .sujet » et « .corps » du modèle
     * @param reference ce que désigne l'e-mail, pour le journal (« rendez-vous 12 », « utilisateur 7 »)
     */
    public void envoyer(String destinataire, Locale langue, String modele, Object[] arguments, String reference) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(expediteur);
        message.setTo(destinataire);
        message.setSubject(textes.getMessage(modele + ".sujet", arguments, langue));
        message.setText(textes.getMessage(modele + ".corps", arguments, langue)
                + "\n\n" + textes.getMessage("signature", null, langue));
        for (int tentative = 1; tentative <= TENTATIVES; tentative++) {
            try {
                messagerie.send(message);
                return;
            } catch (MailException e) {
                journal.warn("E-mail « {} » ({}) non envoyé (tentative {}/{}) : {}",
                        modele, reference, tentative, TENTATIVES, e.getMessage());
            }
            if (tentative < TENTATIVES && !patienter()) {
                return;
            }
        }
        journal.error("E-mail « {} » ({}) abandonné après {} tentatives ; l'opération reste valide",
                modele, reference, TENTATIVES);
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

    /** Textes en français, néerlandais et anglais. */
    private static MessageSource textesDesCourriels() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("courriels/messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
