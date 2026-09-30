package be.immoconnect.dto;

import be.immoconnect.entities.AlerteSecurite;
import java.time.LocalDateTime;

/** Alerte de sécurité telle que la lit l'administrateur. */
public record AlerteResume(Integer id, String type, String ip, String detail, LocalDateTime creeLe) {

    public static AlerteResume depuis(AlerteSecurite alerte) {
        return new AlerteResume(alerte.getId(), alerte.getType().name(), alerte.getIp(), alerte.getDetail(), alerte.getCreeLe());
    }
}
