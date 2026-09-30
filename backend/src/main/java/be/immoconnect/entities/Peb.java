package be.immoconnect.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Classe énergétique du certificat PEB, obligatoire dans toute publicité immobilière (chapitre 11 du
 * rapport). Les noms Java ne peuvent pas contenir « + » : chaque constante porte son étiquette, qui est
 * aussi la valeur de la colonne ENUM bien.peb et celle échangée en JSON.
 */
public enum Peb {
    A_PLUS_PLUS("A++"), A_PLUS("A+"), A("A"), B("B"), C("C"), D("D"), E("E"), F("F"), G("G");

    private final String etiquette;

    Peb(String etiquette) {
        this.etiquette = etiquette;
    }

    @JsonValue
    public String etiquette() {
        return etiquette;
    }

    @JsonCreator
    public static Peb depuis(String etiquette) {
        return Arrays.stream(values())
                .filter(p -> p.etiquette.equalsIgnoreCase(etiquette == null ? "" : etiquette.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Classe PEB inconnue : " + etiquette));
    }
}
