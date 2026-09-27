package be.immoconnect.api.favori;

import be.immoconnect.api.PageReponse;
import be.immoconnect.api.bien.BienResume;
import be.immoconnect.service.ServiceFavori;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Favoris du membre connecté (livrable 15, §6.3). Le membre n'est jamais désigné par l'URL : il vient du jeton. */
@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "jwt")
@Tag(name = "Favoris", description = "Biens mis de côté par le membre connecté")
public class FavoriControleur {

    private static final int TAILLE_MAX = 100;

    private final ServiceFavori service;

    public FavoriControleur(ServiceFavori service) {
        this.service = service;
    }

    @GetMapping("/membres/moi/favoris")
    @Operation(summary = "Mes favoris (membre)", description = "Du plus récemment ajouté au plus ancien ; page (défaut 0), taille (défaut 20, max 100).")
    public PageReponse<BienResume> lister(@AuthenticationPrincipal Jwt jeton,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int taille) {
        return PageReponse.depuis(service.lister(identifiant(jeton),
                PageRequest.of(Math.max(page, 0), Math.clamp(taille, 1, TAILLE_MAX))));
    }

    @PutMapping("/biens/{id}/favori")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ajouter le bien aux favoris (membre)", description = "Idempotent. Bien vendu, loué ou sous option : 409 (RA5).")
    public void ajouter(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id) {
        service.ajouter(identifiant(jeton), id);
    }

    @DeleteMapping("/biens/{id}/favori")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retirer le bien des favoris (membre)", description = "Idempotent.")
    public void retirer(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id) {
        service.retirer(identifiant(jeton), id);
    }

    private static Integer identifiant(Jwt jeton) {
        return Integer.valueOf(jeton.getSubject());
    }
}
