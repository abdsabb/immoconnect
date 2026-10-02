package be.immoconnect.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envoi d'un e-mail à partir d'un modèle traduit (src/main/resources/courriels/messages_xx.properties).
 * Chaque e-mail part en deux versions : HTML, mise en page par {@link MiseEnPageCourriel}, et texte brut.
 * Un envoi qui échoue est retenté ; s'il échoue encore, l'incident est journalisé et l'opération
 * métier qui l'a déclenché reste valide (cas d'erreur E2).
 */
@Component
public class ExpediteurCourriel {

    private static final Logger journal = LoggerFactory.getLogger(ExpediteurCourriel.class);
    static final int TENTATIVES = 3;
    private static final String NOM_EXPEDITEUR = "ImmoConnect";
    private static final String PREFIXE_SUJET = "[ImmoConnect] ";
    private static final String MODELE_CODE = "compte.code_connexion";

    private final JavaMailSender messagerie;
    private final MessageSource textes;
    private final String expediteur;
    private final String urlSite;
    private final Duration delaiEntreTentatives;

    public ExpediteurCourriel(JavaMailSender messagerie,
                              @Value("${immoconnect.courriel.expediteur}") String expediteur,
                              @Value("${immoconnect.courriel.url-site}") String urlSite,
                              @Value("${immoconnect.courriel.delai-entre-tentatives}") Duration delaiEntreTentatives) {
        this.messagerie = messagerie;
        this.textes = textesDesCourriels();
        this.expediteur = expediteur;
        this.urlSite = urlSite;
        this.delaiEntreTentatives = delaiEntreTentatives;
    }

    /**
     * @param modele    préfixe des clés « .sujet » et « .corps » du modèle (et « .bouton », si l'e-mail porte un lien)
     * @param reference ce que désigne l'e-mail, pour le journal (« rendez-vous 12 », « utilisateur 7 »)
     */
    public void envoyer(String destinataire, Locale langue, String modele, Object[] arguments, String reference) {
        MimeMessage message;
        try {
            message = composer(destinataire, langue, modele, arguments);
        } catch (MessagingException | UnsupportedEncodingException e) {
            journal.error("E-mail « {} » ({}) impossible à composer ; l'opération reste valide : {}", modele, reference, e.getMessage());
            return;
        }
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

    private MimeMessage composer(String destinataire, Locale langue, String modele, Object[] arguments)
            throws MessagingException, UnsupportedEncodingException {
        String sujet = textes.getMessage(modele + ".sujet", arguments, langue);
        String corps = textes.getMessage(modele + ".corps", arguments, langue);
        String signature = textes.getMessage("signature", null, langue);
        String bouton = textes.getMessage(modele + ".bouton", null, null, langue);
        String code = MODELE_CODE.equals(modele) ? String.valueOf(arguments[1]) : null;
        String titre = sujet.startsWith(PREFIXE_SUJET) ? sujet.substring(PREFIXE_SUJET.length()) : sujet;

        MimeMessageHelper courriel = new MimeMessageHelper(new MimeMessage((Session) null), true, StandardCharsets.UTF_8.name());
        courriel.setFrom(expediteur, NOM_EXPEDITEUR);
        courriel.setTo(destinataire);
        courriel.setSubject(sujet);
        courriel.setText(corps + "\n\n" + signature,
                MiseEnPageCourriel.html(langue.getLanguage(), titre, corps, bouton, code, signature, urlSite));
        return courriel.getMimeMessage();
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
