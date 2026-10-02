package be.immoconnect.controllers;

import static be.immoconnect.controllers.RequeteHttp.adresseIp;

import be.immoconnect.dto.BienGestion;
import be.immoconnect.dto.RequeteBien;
import be.immoconnect.exceptions.DonneeInvalideException;
import be.immoconnect.services.ServiceAnnonces;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Back-office de l'agent (livrable 15, §6.2) : ses annonces et leurs photos. Le rôle agent est exigé
 * par la configuration de sécurité ; la propriété de chaque annonce est vérifiée par le service.
 */
@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "jwt")
@Tag(name = "Annonces", description = "Gestion de ses annonces par l'agent immobilier")
public class AnnonceControleur {

    private final ServiceAnnonces service;

    public AnnonceControleur(ServiceAnnonces service) {
        this.service = service;
    }

    @GetMapping("/agents/moi/biens")
    @Operation(summary = "Mes annonces et leur tableau de bord (agent)",
            description = "Toutes les annonces de l'agent, hors ligne comprises, avec favoris, demandes en attente et visites à venir.")
    public List<BienGestion> mesAnnonces(@AuthenticationPrincipal Jwt jeton) {
        return service.mesAnnonces(identifiant(jeton));
    }

    @GetMapping("/agents/moi/biens/{id}")
    @Operation(summary = "Une de mes annonces, adresse exacte comprise (agent responsable)")
    public BienGestion annonce(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id) {
        return service.annonce(identifiant(jeton), id);
    }

    @PostMapping("/biens")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une annonce (agent)",
            description = "L'annonce est créée hors ligne : elle se publie une fois sa première photo ajoutée (RA6).")
    public BienGestion creer(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequeteBien requete, HttpServletRequest http) {
        return service.creer(identifiant(jeton), requete, adresseIp(http));
    }

    @PutMapping("/biens/{id}")
    @Operation(summary = "Modifier une annonce et son statut (agent responsable)",
            description = "Publier exige au moins une photo (RA6). Vendu, loué ou archivé : refusé tant que des visites sont prévues (409).")
    public BienGestion modifier(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id,
                                @Valid @RequestBody RequeteBien requete, HttpServletRequest http) {
        return service.modifier(identifiant(jeton), id, requete, adresseIp(http));
    }

    @DeleteMapping("/biens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archiver une annonce (agent responsable)",
            description = "Suppression logique : l'annonce quitte la recherche et reste dans l'historique du back-office.")
    public void archiver(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        service.archiver(identifiant(jeton), id, adresseIp(http));
    }

    @DeleteMapping("/biens/{id}/definitif")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer définitivement une annonce (agent responsable ou administrateur gestionnaire)",
            description = "L'annonce, ses photos et ses favoris sont supprimés (RA10). Refusé (409) si l'annonce a un historique de visites : elle se met alors hors ligne.")
    public void supprimer(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        service.supprimer(identifiant(jeton), id, adresseIp(http));
    }

    @PostMapping(path = "/biens/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajouter une photo (agent responsable)",
            description = "JPEG ou PNG, 5 Mo au plus. Le type est vérifié sur le contenu et l'image est ré-encodée en JPEG.")
    public BienGestion ajouterPhoto(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id,
                                    @RequestParam("fichier") MultipartFile fichier,
                                    @RequestParam(required = false) String legende, HttpServletRequest http) {
        try {
            return service.ajouterPhoto(identifiant(jeton), id, fichier.getBytes(), legende, adresseIp(http));
        } catch (IOException e) {
            throw new DonneeInvalideException("fichier", "le fichier n'a pas pu être lu");
        }
    }

    @DeleteMapping("/biens/{id}/photos/{photoId}")
    @Operation(summary = "Supprimer une photo (agent responsable)", description = "La dernière photo d'une annonce en ligne ne se supprime pas (RA6).")
    public BienGestion supprimerPhoto(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id,
                                      @PathVariable Integer photoId, HttpServletRequest http) {
        return service.supprimerPhoto(identifiant(jeton), id, photoId, adresseIp(http));
    }

    @PutMapping("/biens/{id}/photos/{photoId}/couverture")
    @Operation(summary = "Choisir la photo de couverture (agent responsable)")
    public BienGestion definirCouverture(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id,
                                         @PathVariable Integer photoId, HttpServletRequest http) {
        return service.definirCouverture(identifiant(jeton), id, photoId, adresseIp(http));
    }

    private static Integer identifiant(Jwt jeton) {
        return Integer.valueOf(jeton.getSubject());
    }
}
