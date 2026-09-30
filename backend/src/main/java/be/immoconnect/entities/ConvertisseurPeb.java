package be.immoconnect.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Colonne ENUM bien.peb : la base garde l'étiquette (« A+ »), pas le nom de la constante Java. */
@Converter(autoApply = true)
public class ConvertisseurPeb implements AttributeConverter<Peb, String> {

    @Override
    public String convertToDatabaseColumn(Peb peb) {
        return peb == null ? null : peb.etiquette();
    }

    @Override
    public Peb convertToEntityAttribute(String etiquette) {
        return etiquette == null ? null : Peb.depuis(etiquette);
    }
}
