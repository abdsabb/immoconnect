package be.immoconnect.controllers;

import static be.immoconnect.controllers.RequeteHttp.adresseIp;

import be.immoconnect.dto.ArticleVue;
import be.immoconnect.dto.CategorieBlog;
import be.immoconnect.dto.PageReponse;
import be.immoconnect.dto.RequeteArticle;
import be.immoconnect.entities.StatutArticle;
import be.immoconnect.exceptions.DonneeInvalideException;
import be.immoconnect.services.ServiceBlog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Blog (livrable 15, §6.7) : lecture publique sous /articles, gestion sous /admin/articles. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Blog", description = "Articles d'actualité immobilière")
public class BlogControleur {

    private static final int TAILLE_MAX = 50;

    private final ServiceBlog service;

    public BlogControleur(ServiceBlog service) {
        this.service = service;
    }

    @GetMapping("/articles")
    @Operation(summary = "Articles publiés", description = "Du plus récent au plus ancien ; filtre facultatif par catégorie.")
    public PageReponse<ArticleVue> publies(@RequestParam(required = false) Integer categorieId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "9") int taille) {
        return PageReponse.depuis(service.publies(categorieId, pagination(page, taille, "publieLe")));
    }

    @GetMapping("/articles/categories")
    @Operation(summary = "Catégories du blog")
    public List<CategorieBlog> categories() {
        return service.categories();
    }

    @GetMapping("/articles/{id}")
    @Operation(summary = "Un article publié", description = "Un brouillon ou un article archivé est introuvable pour le public (RA4).")
    public ArticleVue publie(@PathVariable Integer id) {
        return service.publie(id);
    }

    @GetMapping("/admin/articles")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Tous les articles, brouillons compris (administrateur)")
    public PageReponse<ArticleVue> tous(@AuthenticationPrincipal Jwt jeton,
                                        @RequestParam(required = false) StatutArticle statut,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int taille) {
        return PageReponse.depuis(service.tous(identifiant(jeton), statut, pagination(page, taille, "id")));
    }

    @GetMapping("/admin/articles/{id}")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Un article, quel que soit son statut (administrateur)")
    public ArticleVue article(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id) {
        return service.article(identifiant(jeton), id);
    }

    @PostMapping("/admin/articles")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Rédiger un article (administrateur)", description = "L'article est créé à l'état de brouillon.")
    public ArticleVue creer(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequeteArticle requete, HttpServletRequest http) {
        return service.creer(identifiant(jeton), requete, adresseIp(http));
    }

    @PutMapping("/admin/articles/{id}")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Modifier un article (administrateur)")
    public ArticleVue modifier(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id,
                               @Valid @RequestBody RequeteArticle requete, HttpServletRequest http) {
        return service.modifier(identifiant(jeton), id, requete, adresseIp(http));
    }

    @PatchMapping("/admin/articles/{id}/publier")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Publier un article (administrateur)", description = "La date de publication est renseignée automatiquement (RA4).")
    public ArticleVue publier(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        return service.publier(identifiant(jeton), id, adresseIp(http));
    }

    @PatchMapping("/admin/articles/{id}/archiver")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Archiver un article (administrateur)")
    public ArticleVue archiver(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        return service.archiver(identifiant(jeton), id, adresseIp(http));
    }

    @PostMapping(path = "/admin/articles/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Choisir l'image de couverture d'un article (administrateur)",
            description = "JPEG ou PNG, 5 Mo au plus. Le type est vérifié sur le contenu et l'image est ré-encodée en JPEG ; elle remplace la précédente.")
    public ArticleVue definirImage(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id,
                                   @RequestParam("fichier") MultipartFile fichier, HttpServletRequest http) {
        try {
            return service.definirImage(identifiant(jeton), id, fichier.getBytes(), adresseIp(http));
        } catch (IOException e) {
            throw new DonneeInvalideException("fichier", "le fichier n'a pas pu être lu");
        }
    }

    @DeleteMapping("/admin/articles/{id}/image")
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Retirer l'image de couverture d'un article (administrateur)")
    public ArticleVue retirerImage(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        return service.retirerImage(identifiant(jeton), id, adresseIp(http));
    }

    @DeleteMapping("/admin/articles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "jwt")
    @Operation(summary = "Supprimer un article (administrateur)")
    public void supprimer(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        service.supprimer(identifiant(jeton), id, adresseIp(http));
    }

    private static PageRequest pagination(int page, int taille, String tri) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(taille, 1, TAILLE_MAX), Sort.by(Sort.Direction.DESC, tri, "id"));
    }

    private static Integer identifiant(Jwt jeton) {
        return Integer.valueOf(jeton.getSubject());
    }
}
