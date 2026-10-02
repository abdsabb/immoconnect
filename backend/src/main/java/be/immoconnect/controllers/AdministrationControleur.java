package be.immoconnect.controllers;

import static be.immoconnect.controllers.RequeteHttp.adresseIp;

import be.immoconnect.dto.AlerteResume;
import be.immoconnect.dto.BienGestion;
import be.immoconnect.dto.CategorieResume;
import be.immoconnect.dto.CleApiResume;
import be.immoconnect.dto.CompteResume;
import be.immoconnect.dto.ConversationResume;
import be.immoconnect.dto.DemandeContactResume;
import be.immoconnect.dto.MessageResume;
import be.immoconnect.dto.PageReponse;
import be.immoconnect.dto.ParametreLigne;
import be.immoconnect.dto.RequeteAgent;
import be.immoconnect.dto.RequeteCategorie;
import be.immoconnect.dto.RequeteCleApi;
import be.immoconnect.dto.RequeteParametres;
import be.immoconnect.dto.Statistiques;
import be.immoconnect.dto.TraceAudit;
import be.immoconnect.dto.TraductionLigne;
import be.immoconnect.services.ServiceAlertesSecurite;
import be.immoconnect.services.ServiceAnnonces;
import be.immoconnect.services.ServiceCategories;
import be.immoconnect.services.ServiceClesApi;
import be.immoconnect.services.ServiceComptes;
import be.immoconnect.services.ServiceContact;
import be.immoconnect.services.ServiceJournal;
import be.immoconnect.services.ServiceMessagerie;
import be.immoconnect.services.ServiceParametres;
import be.immoconnect.services.ServiceStatistiques;
import be.immoconnect.services.ServiceTraductions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

/**
 * Back-office de l'administrateur (contrainte de l'épreuve) : comptes (A1), catégories (A3),
 * journal d'audit (A4) et statistiques (A8). Tout le préfixe /admin exige le rôle administrateur ;
 * chaque service vérifie ensuite le niveau d'accès.
 */
@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "jwt")
@Tag(name = "Administration", description = "Back-office de l'administrateur")
public class AdministrationControleur {

    private static final int TAILLE_MAX = 100;

    private final ServiceComptes comptes;
    private final ServiceJournal journal;
    private final ServiceStatistiques statistiques;
    private final ServiceCategories categories;
    private final ServiceClesApi clesApi;
    private final ServiceTraductions traductions;
    private final ServiceParametres parametres;
    private final ServiceAlertesSecurite alertes;
    private final ServiceAnnonces annonces;
    private final ServiceMessagerie messagerie;
    private final ServiceContact contact;

    public AdministrationControleur(ServiceComptes comptes, ServiceJournal journal, ServiceStatistiques statistiques,
                                    ServiceCategories categories, ServiceClesApi clesApi, ServiceTraductions traductions,
                                    ServiceParametres parametres,
                                    ServiceAlertesSecurite alertes, ServiceAnnonces annonces, ServiceMessagerie messagerie,
                                    ServiceContact contact) {
        this.contact = contact;
        this.alertes = alertes;
        this.annonces = annonces;
        this.messagerie = messagerie;
        this.clesApi = clesApi;
        this.traductions = traductions;
        this.parametres = parametres;
        this.comptes = comptes;
        this.journal = journal;
        this.statistiques = statistiques;
        this.categories = categories;
    }

    // ---------- A1 — Comptes ----------

    @GetMapping("/utilisateurs")
    @Operation(summary = "Comptes de la plateforme", description = "Filtres : role (membre, agent, admin) et recherche sur le nom, le prénom ou l'e-mail.")
    public PageReponse<CompteResume> utilisateurs(@AuthenticationPrincipal Jwt jeton,
                                                  @RequestParam(required = false) String role,
                                                  @RequestParam(required = false) String recherche,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int taille) {
        var pagination = PageRequest.of(Math.max(page, 0), Math.clamp(taille, 1, TAILLE_MAX), Sort.by("nom", "prenom", "id"));
        return PageReponse.depuis(comptes.lister(identifiant(jeton), role, recherche, pagination));
    }

    @PostMapping("/agents")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ouvrir le compte d'un agent", description = "Le matricule est attribué par le serveur ; l'agent change ensuite son mot de passe provisoire.")
    public CompteResume creerAgent(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequeteAgent requete, HttpServletRequest http) {
        return comptes.creerAgent(identifiant(jeton), requete, adresseIp(http));
    }

    @PatchMapping("/utilisateurs/{id}/desactiver")
    @Operation(summary = "Désactiver un compte", description = "Le compte ne peut plus se connecter ; son historique est conservé.")
    public CompteResume desactiver(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        return comptes.changerActivation(identifiant(jeton), id, false, adresseIp(http));
    }

    @PatchMapping("/utilisateurs/{id}/activer")
    @Operation(summary = "Réactiver un compte")
    public CompteResume activer(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        return comptes.changerActivation(identifiant(jeton), id, true, adresseIp(http));
    }

    // ---------- A5 — Paramètres du site ----------

    @GetMapping("/parametres")
    @Operation(summary = "Paramètres du site", description = "Nom, slogan, coordonnées et horaires de l'agence, langues actives (niveau 2).")
    public List<ParametreLigne> parametres(@AuthenticationPrincipal Jwt jeton) {
        return parametres.lister(identifiant(jeton));
    }

    @PutMapping("/parametres")
    @Operation(summary = "Modifier des paramètres", description = "Valeurs par clé ; une clé inconnue ou une valeur invalide est refusée (422). Chaque changement est journalisé.")
    public List<ParametreLigne> enregistrerParametres(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequeteParametres requete,
                                                      HttpServletRequest http) {
        return parametres.enregistrer(identifiant(jeton), requete, adresseIp(http));
    }

    // ---------- Supervision des agents : annonces et messagerie ----------

    @GetMapping("/biens")
    @Operation(summary = "Annonces de tous les agents",
            description = "Hors ligne comprises, avec leurs indicateurs ; filtre : agentId. La modification passe par PUT /biens/{id}, ouvert à l'administrateur gestionnaire. Niveau 2.")
    public List<BienGestion> annonces(@AuthenticationPrincipal Jwt jeton, @RequestParam(required = false) Integer agentId) {
        return annonces.toutes(identifiant(jeton), agentId);
    }

    @GetMapping("/biens/{id}")
    @Operation(summary = "Une annonce, adresse exacte comprise", description = "Niveau 2.")
    public BienGestion annonce(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id) {
        return annonces.annonce(identifiant(jeton), id);
    }

    @GetMapping("/agents/{agentId}/conversations")
    @Operation(summary = "Conversations d'un agent", description = "Lecture seule. Niveau 2.")
    public List<ConversationResume> conversations(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer agentId) {
        return messagerie.conversationsDeLAgent(identifiant(jeton), agentId);
    }

    @GetMapping("/agents/{agentId}/conversations/{membreId}")
    @Operation(summary = "Messages échangés entre un agent et un membre",
            description = "Lecture seule : rien n'est marqué comme lu. La consultation est journalisée. Niveau 2.")
    public List<MessageResume> conversation(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer agentId,
                                            @PathVariable Integer membreId, HttpServletRequest http) {
        return messagerie.conversationDeLAgent(identifiant(jeton), agentId, membreId, adresseIp(http));
    }

    // ---------- Demandes du formulaire de contact ----------

    @GetMapping("/contacts")
    @Operation(summary = "Demandes de contact", description = "De la plus récente à la plus ancienne ; enAttente=true ne garde que celles sans réponse. Niveau 2.")
    public PageReponse<DemandeContactResume> contacts(@AuthenticationPrincipal Jwt jeton,
                                                      @RequestParam(defaultValue = "false") boolean enAttente,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int taille) {
        return PageReponse.depuis(contact.lister(identifiant(jeton), enAttente, PageRequest.of(Math.max(page, 0), Math.clamp(taille, 1, TAILLE_MAX))));
    }

    @PatchMapping("/contacts/{id}/traiter")
    @Operation(summary = "Marquer une demande de contact comme traitée", description = "Journalisé au nom de l'administrateur. Niveau 2.")
    public DemandeContactResume traiterContact(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        return contact.traiter(identifiant(jeton), id, adresseIp(http));
    }

    // ---------- A4 — Journal d'audit ----------

    @GetMapping("/journal")
    @Operation(summary = "Journal d'audit", description = "Du plus récent au plus ancien. Filtres : action, utilisateurId, du, au (AAAA-MM-JJ).")
    public PageReponse<TraceAudit> journal(@AuthenticationPrincipal Jwt jeton,
                                           @RequestParam(required = false) String action,
                                           @RequestParam(required = false) Integer utilisateurId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int taille) {
        var pagination = PageRequest.of(Math.max(page, 0), Math.clamp(taille, 1, TAILLE_MAX),
                Sort.by(Sort.Direction.DESC, "horodatage", "id"));
        return PageReponse.depuis(journal.consulter(identifiant(jeton), action, utilisateurId, du, au, pagination));
    }

    @GetMapping("/alertes")
    @Operation(summary = "Alertes de sécurité",
            description = "Détection d'intrusion : rafales d'échecs de connexion, énumération d'identifiants, clés API révoquées présentées. Niveau 2.")
    public PageReponse<AlerteResume> alertes(@AuthenticationPrincipal Jwt jeton,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int taille) {
        return PageReponse.depuis(alertes.lister(identifiant(jeton), PageRequest.of(Math.max(page, 0), Math.clamp(taille, 1, TAILLE_MAX))));
    }

    @GetMapping("/journal/actions")
    @Operation(summary = "Actions présentes dans le journal", description = "Valeurs possibles du filtre « action ».")
    public List<String> actions(@AuthenticationPrincipal Jwt jeton) {
        return journal.actions(identifiant(jeton));
    }

    // ---------- A8 — Statistiques ----------

    @GetMapping("/statistiques")
    @Operation(summary = "Statistiques du site", description = "Biens, comptes, rendez-vous, revenus des créneaux premium, prix par commune.")
    public Statistiques statistiques(@AuthenticationPrincipal Jwt jeton) {
        return statistiques.calculer(identifiant(jeton));
    }

    // ---------- A7 — Clés API ----------

    @GetMapping("/cles-api")
    @Operation(summary = "Clés API délivrées", description = "Libellé, date de création, état et dernière utilisation. La clé elle-même n'est jamais renvoyée.")
    public List<CleApiResume> clesApi(@AuthenticationPrincipal Jwt jeton) {
        return clesApi.lister(identifiant(jeton));
    }

    @PostMapping("/cles-api")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Générer une clé API", description = "La clé figure dans cette réponse et nulle part ailleurs : elle ne pourra plus être affichée.")
    public CleApiResume genererCleApi(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequeteCleApi requete, HttpServletRequest http) {
        return clesApi.generer(identifiant(jeton), requete, adresseIp(http));
    }

    @PatchMapping("/cles-api/{id}/revoquer")
    @Operation(summary = "Révoquer une clé API", description = "Effet immédiat et définitif (RA12).")
    public CleApiResume revoquerCleApi(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        return clesApi.revoquer(identifiant(jeton), id, adresseIp(http));
    }

    // ---------- A6 — Langues et traductions ----------

    @GetMapping("/traductions")
    @Operation(summary = "Textes du site dans toutes les langues", description = "Une ligne par clé, avec sa valeur en français, néerlandais et anglais.")
    public List<TraductionLigne> traductions(@AuthenticationPrincipal Jwt jeton) {
        return traductions.lister(identifiant(jeton));
    }

    @PutMapping("/traductions/{cle}")
    @Operation(summary = "Créer ou modifier un texte du site", description = "Seules les langues transmises sont modifiées. Effet immédiat sur le site public.")
    public TraductionLigne enregistrerTraduction(@AuthenticationPrincipal Jwt jeton, @PathVariable String cle,
                                                 @Valid @RequestBody TraductionLigne.Requete requete, HttpServletRequest http) {
        return traductions.enregistrer(identifiant(jeton), cle, requete, adresseIp(http));
    }

    @DeleteMapping("/traductions/{cle}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un texte du site dans toutes les langues")
    public void supprimerTraduction(@AuthenticationPrincipal Jwt jeton, @PathVariable String cle, HttpServletRequest http) {
        traductions.supprimer(identifiant(jeton), cle, adresseIp(http));
    }

    // ---------- A3 — Catégories de biens ----------

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une catégorie de biens")
    public CategorieResume creerCategorie(@AuthenticationPrincipal Jwt jeton, @Valid @RequestBody RequeteCategorie requete,
                                          HttpServletRequest http) {
        return categories.creer(identifiant(jeton), requete, adresseIp(http));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Modifier une catégorie de biens")
    public CategorieResume modifierCategorie(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id,
                                             @Valid @RequestBody RequeteCategorie requete, HttpServletRequest http) {
        return categories.modifier(identifiant(jeton), id, requete, adresseIp(http));
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une catégorie de biens", description = "Refusé (409) tant que des biens portent cette catégorie.")
    public void supprimerCategorie(@AuthenticationPrincipal Jwt jeton, @PathVariable Integer id, HttpServletRequest http) {
        categories.supprimer(identifiant(jeton), id, adresseIp(http));
    }

    private static Integer identifiant(Jwt jeton) {
        return Integer.valueOf(jeton.getSubject());
    }
}
