package be.immoconnect.controllers;

import static be.immoconnect.controllers.RequeteHttp.adresseIp;

import be.immoconnect.dto.RequeteContact;
import be.immoconnect.services.ServiceContact;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Formulaire de contact du site, ouvert à tous les visiteurs. */
@RestController
@RequestMapping("/api/v1/contact")
@Tag(name = "Contact", description = "Écrire à l'agence")
public class ContactControleur {

    private final ServiceContact service;

    public ContactControleur(ServiceContact service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Envoyer un message à l'agence",
            description = "La demande est enregistrée et transmise par e-mail à l'agence. Cinq messages par heure et par adresse IP au plus (429).")
    public void envoyer(@Valid @RequestBody RequeteContact requete, HttpServletRequest http) {
        service.envoyer(requete, adresseIp(http));
    }
}
