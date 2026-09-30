package be.immoconnect.dto;

import be.immoconnect.entities.Parametre;
import java.time.LocalDateTime;

/** Un paramètre du site et sa dernière modification (cas A5). */
public record ParametreLigne(String cle, String valeur, LocalDateTime modifieLe, String modifiePar) {

    public static ParametreLigne depuis(Parametre p) {
        return new ParametreLigne(p.getCle(), p.getValeur(), p.getModifieLe(),
                p.getModifiePar() == null ? null : p.getModifiePar().getNomComplet());
    }
}
