package be.immoconnect.api;

import java.util.List;
import org.springframework.data.domain.Page;

/** Enveloppe de pagination documentée au livrable 15 (§5) : contenu, page, taille, totalElements, totalPages. */
public record PageReponse<T>(List<T> contenu, int page, int taille, long totalElements, int totalPages) {

    public static <T> PageReponse<T> depuis(Page<T> page) {
        return new PageReponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
