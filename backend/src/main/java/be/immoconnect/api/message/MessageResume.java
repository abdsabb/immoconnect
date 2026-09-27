package be.immoconnect.api.message;

import be.immoconnect.entite.Message;
import java.time.LocalDateTime;

/** Message d'une conversation ; {@code deMoi} évite au client de comparer des identifiants. */
public record MessageResume(Integer id, String expediteur, String destinataire, String contenu, LocalDateTime envoyeLe,
                            boolean lu, boolean deMoi) {

    public static MessageResume depuis(Message message, Integer lecteurId) {
        return new MessageResume(message.getId(), message.getAuteur().getNomComplet(),
                message.getDestinataire().getNomComplet(), message.getContenu(), message.getEnvoyeLe(), message.isLu(),
                message.getAuteur().getId().equals(lecteurId));
    }
}
