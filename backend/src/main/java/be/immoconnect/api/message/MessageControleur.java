package be.immoconnect.api.message;

import static be.immoconnect.api.RequeteHttp.adresseIp;

import be.immoconnect.securite.ConfigurationJwt;
import be.immoconnect.service.ServiceMessagerie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Messagerie interne entre membres et agents (livrable 15, §6.4). */
@RestController
@RequestMapping("/api/v1/messages")
@SecurityRequirement(name = "jwt")
@Tag(name = "Messages", description = "Conversations entre un membre et un agent immobilier")
public class MessageControleur {

    private final ServiceMessagerie service;

    public MessageControleur(ServiceMessagerie service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Mes conversations (membre ou agent)", description = "Une ligne par interlocuteur, la plus récemment active en premier.")
    public List<ConversationResume> conversations(@AuthenticationPrincipal Jwt jeton) {
        return service.conversations(identifiant(jeton), role(jeton));
    }

    @GetMapping("/conversations/{interlocuteurId}")
    @Operation(summary = "Messages échangés avec un interlocuteur", description = "Dans l'ordre chronologique. Vide si rien n'a encore été échangé.")
    public List<MessageResume> conversation(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer interlocuteurId) {
        return service.conversation(identifiant(jeton), role(jeton), interlocuteurId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envoyer un message", description = "Un membre écrit à un agent ; un agent répond à un membre qui lui a déjà écrit.")
    public MessageResume envoyer(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequeteMessage requete,
                                 HttpServletRequest http) {
        return service.envoyer(identifiant(jeton), role(jeton), requete, adresseIp(http));
    }

    @PatchMapping("/conversations/{interlocuteurId}/lu")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marquer comme lus les messages reçus d'un interlocuteur")
    public void marquerLue(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer interlocuteurId) {
        service.marquerLue(identifiant(jeton), role(jeton), interlocuteurId);
    }

    private static Integer identifiant(Jwt jeton) {
        return Integer.valueOf(jeton.getSubject());
    }

    private static String role(Jwt jeton) {
        return jeton.getClaimAsString(ConfigurationJwt.CLAIM_ROLE);
    }
}
