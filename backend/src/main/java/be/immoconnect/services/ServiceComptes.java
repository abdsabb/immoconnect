package be.immoconnect.services;

import be.immoconnect.dto.CompteResume;
import be.immoconnect.dto.RequeteAgent;
import be.immoconnect.entities.Administrateur;
import be.immoconnect.entities.AgentImmobilier;
import be.immoconnect.entities.Langue;
import be.immoconnect.entities.Membre;
import be.immoconnect.entities.Utilisateur;
import be.immoconnect.exceptions.DonneeInvalideException;
import be.immoconnect.exceptions.OperationInterditeException;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.repositories.AgentImmobilierRepository;
import be.immoconnect.repositories.LangueRepository;
import be.immoconnect.repositories.UtilisateurRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cas d'utilisation « Gérer les utilisateurs » (A1) : consulter, ouvrir un compte agent, activer, désactiver. */
@Service
public class ServiceComptes {

    private static final Map<String, Class<? extends Utilisateur>> TYPES = Map.of(
            "membre", Membre.class, "agent", AgentImmobilier.class, "admin", Administrateur.class);

    private final UtilisateurRepository utilisateurs;
    private final AgentImmobilierRepository agents;
    private final LangueRepository langues;
    private final PasswordEncoder encodeur;
    private final AccesAdministrateur acces;
    private final ServiceAudit audit;
    private final Clock horloge;

    public ServiceComptes(UtilisateurRepository utilisateurs, AgentImmobilierRepository agents, LangueRepository langues,
                          PasswordEncoder encodeur, AccesAdministrateur acces, ServiceAudit audit, Clock horloge) {
        this.utilisateurs = utilisateurs;
        this.agents = agents;
        this.langues = langues;
        this.encodeur = encodeur;
        this.acces = acces;
        this.audit = audit;
        this.horloge = horloge;
    }

    @Transactional(readOnly = true)
    public Page<CompteResume> lister(Integer administrateurId, String role, String recherche, Pageable pagination) {
        acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        if (role != null && !role.isBlank() && !TYPES.containsKey(role)) {
            throw new DonneeInvalideException("role", "rôle inconnu");
        }
        Specification<Utilisateur> filtre = Specification.allOf(parRole(role), parTexte(recherche));
        return utilisateurs.findAll(filtre, pagination).map(CompteResume::depuis);
    }

    @Transactional
    public CompteResume creerAgent(Integer administrateurId, RequeteAgent requete, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        String email = requete.email().trim().toLowerCase();
        if (utilisateurs.existsByEmailIgnoreCase(email)) {
            throw new ServiceAuthentification.EmailDejaUtiliseException();
        }
        String codeLangue = requete.langue() == null || requete.langue().isBlank() ? "fr" : requete.langue();
        Langue langue = langues.findByCode(codeLangue).orElseThrow(() -> new RessourceIntrouvableException("Langue", codeLangue));

        AgentImmobilier agent = new AgentImmobilier(requete.nom().trim(), requete.prenom().trim(), email,
                encodeur.encode(requete.motDePasse()), langue, prochainMatricule(), requete.telephonePro().trim());
        utilisateurs.save(agent);
        audit.enregistrer(administrateur, "creation_compte_agent", "utilisateur#" + agent.getId(), ip);
        return CompteResume.depuis(agent);
    }

    /**
     * Un compte désactivé ne peut plus se connecter ; ses annonces, rendez-vous et paiements restent en place.
     * Nul ne se désactive lui-même, et seul un super-administrateur agit sur le compte d'un administrateur.
     */
    @Transactional
    public CompteResume changerActivation(Integer administrateurId, Integer compteId, boolean actif, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        Utilisateur compte = utilisateurs.findById(compteId).orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", compteId));
        if (compte.getId().equals(administrateurId)) {
            throw new OperationInterditeException("Vous ne pouvez pas désactiver votre propre compte");
        }
        if (compte instanceof Administrateur) {
            acces.exiger(administrateurId, AccesAdministrateur.SUPER_ADMINISTRATEUR);
        }
        if (compte.isActif() != actif) {
            compte.setActif(actif);
            audit.enregistrer(administrateur, actif ? "activation_compte" : "desactivation_compte", "utilisateur#" + compteId, ip);
        }
        return CompteResume.depuis(compte);
    }

    /** Matricule AG-année-numéro, dans la continuité de ceux de l'agence. */
    private String prochainMatricule() {
        String prefixe = "AG-" + LocalDate.now(horloge).getYear() + "-";
        long rang = agents.countByMatriculeStartingWith(prefixe) + 1;
        String matricule = prefixe + String.format("%03d", rang);
        while (agents.existsByMatricule(matricule)) {
            matricule = prefixe + String.format("%03d", ++rang);
        }
        return matricule;
    }

    private static Specification<Utilisateur> parRole(String role) {
        return (racine, requete, cb) -> role == null || role.isBlank() ? null : cb.equal(racine.type(), TYPES.get(role));
    }

    /** Recherche sur le nom, le prénom ou l'adresse e-mail. La saisie est un paramètre de requête, jamais du SQL. */
    private static Specification<Utilisateur> parTexte(String recherche) {
        return (racine, requete, cb) -> {
            if (recherche == null || recherche.isBlank()) {
                return null;
            }
            String motif = "%" + recherche.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            return cb.or(cb.like(cb.lower(racine.get("nom")), motif, '\\'),
                    cb.like(cb.lower(racine.get("prenom")), motif, '\\'),
                    cb.like(cb.lower(racine.get("email")), motif, '\\'));
        };
    }
}
