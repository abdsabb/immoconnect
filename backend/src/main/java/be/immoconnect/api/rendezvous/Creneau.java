package be.immoconnect.api.rendezvous;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Créneau de visite encore libre : standard (gratuit) ou premium (soirée / week-end, payant). */
public record Creneau(LocalDateTime dateHeure, Type type, BigDecimal prix) {

    public enum Type {
        standard, premium
    }
}
