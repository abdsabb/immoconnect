package be.immoconnect;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.List;

/** Lecture d'un e-mail remis à la messagerie simulée : destinataires, sujet, version texte et version HTML. */
public record CourrielRecu(List<String> destinataires, String sujet, String texte, String html) {

    public static CourrielRecu de(MimeMessage message) {
        try {
            // Les en-têtes de chaque partie (dont son type) ne sont écrits qu'à l'enregistrement, que fait l'envoi réel
            message.saveChanges();
            Address[] destinataires = message.getRecipients(Message.RecipientType.TO);
            return new CourrielRecu(
                    destinataires == null ? List.of() : Arrays.stream(destinataires).map(Address::toString).toList(),
                    message.getSubject(), partie(message, "text/plain"), partie(message, "text/html"));
        } catch (Exception e) {
            throw new IllegalStateException("E-mail illisible", e);
        }
    }

    private static String partie(Part partie, String type) throws Exception {
        Object contenu = partie.getContent();
        if (contenu instanceof Multipart parties) {
            for (int i = 0; i < parties.getCount(); i++) {
                String trouve = partie(parties.getBodyPart(i), type);
                if (trouve != null) {
                    return trouve;
                }
            }
            return null;
        }
        return partie.isMimeType(type) ? String.valueOf(contenu) : null;
    }
}
