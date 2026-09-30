package be.immoconnect.dto;

import be.immoconnect.entities.CleApi;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Clé API vue par l'administrateur (cas A7). {@code cle} n'est renseignée qu'une fois, dans la
 * réponse à la création : la base n'en garde que l'empreinte, elle ne peut plus être affichée ensuite.
 */
public record CleApiResume(Integer id, String libelle, LocalDate creeLe, boolean active, LocalDateTime derniereUtilisation,
                           String creeePar, String cle) {

    public static CleApiResume depuis(CleApi cle) {
        return nouvelle(cle, null);
    }

    public static CleApiResume nouvelle(CleApi cle, String valeur) {
        return new CleApiResume(cle.getId(), cle.getLibelle(), cle.getCreeLe(), cle.isActive(), cle.getDerniereUtilisation(),
                cle.getAdministrateur().getNomComplet(), valeur);
    }
}
