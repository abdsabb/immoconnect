package be.immoconnect.dto;

import java.time.LocalDateTime;

/** Une ligne de la liste des conversations : l'interlocuteur, le dernier message, les messages à lire. */
public record ConversationResume(Integer interlocuteurId, String interlocuteur, String dernierMessage,
                                 LocalDateTime dernierEnvoi, boolean dernierDeMoi, long nonLus) {
}
