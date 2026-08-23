package be.immoconnect.service;

import be.immoconnect.api.auth.RequeteChangementMotDePasse;
import be.immoconnect.api.auth.RequeteModificationProfil;
import be.immoconnect.api.auth.UtilisateurResume;
import be.immoconnect.depot.FavoriRepository;
import be.immoconnect.depot.LangueRepository;
import be.immoconnect.depot.MessageRepository;
import be.immoconnect.depot.UtilisateurRepository;
import be.immoconnect.entite.Membre;
import be.immoconnect.entite.Message;
import be.immoconnect.entite.Utilisateur;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cas d'utilisation Modifier son profil (M6) et Se désinscrire (M7). */
@Service
public class ServiceProfil {

    private final UtilisateurRepository utilisateurs;
    private final LangueRepository langues;
    private final FavoriRepository favoris;
    private final MessageRepository messages;
    private final PasswordEncoder encodeur;
    private final ServiceAudit audit;

    public ServiceProfil(UtilisateurRepository utilisateurs, LangueRepository langues, FavoriRepository favoris,
                         MessageRepository messages, PasswordEncoder encodeur, ServiceAudit audit) {
        this.utilisateurs = utilisateurs;
        this.langues = langues;
        this.favoris = favoris;
        this.messages = messages;
        this.encodeur = encodeur;
        this.audit = audit;
    }

    @Transactional
    public UtilisateurResume modifier(Integer id, RequeteModificationProfil requete, String ip) {
        Utilisateur utilisateur = charger(id);
        utilisateur.setNom(requete.nom().trim());
        utilisateur.setPrenom(requete.prenom().trim());
        utilisateur.setLangue(langues.findByCode(requete.langue())
                .orElseThrow(() -> new RessourceIntrouvableException("Langue", requete.langue())));
        if (utilisateur instanceof Membre membre) {
            membre.setTelephone(requete.telephone() == null || requete.telephone().isBlank() ? null : requete.telephone().trim());
        }
        audit.enregistrer(utilisateur, "modification_profil", "utilisateur#" + id, ip);
        return UtilisateurResume.depuis(utilisateur);
    }

    @Transactional
    public void changerMotDePasse(Integer id, RequeteChangementMotDePasse requete, String ip) {
        Utilisateur utilisateur = charger(id);
        if (!encodeur.matches(requete.ancienMotDePasse(), utilisateur.getMotDePasse())) {
            throw new BadCredentialsException("Identifiants invalides");
        }
        utilisateur.setMotDePasse(encodeur.encode(requete.nouveauMotDePasse()));
        audit.enregistrer(utilisateur, "changement_mot_de_passe", "utilisateur#" + id, ip);
    }

    /**
     * Désinscription d'un membre — droit à l'oubli du RGPD, règle RA11 (« soft delete »).
     * <p>
     * Un membre qui a pris des rendez-vous ou payé des créneaux ne peut pas être effacé physiquement :
     * les rendez-vous, paiements et entrées d'audit y font référence (clés étrangères RESTRICT) et
     * les pièces de paiement doivent être conservées pour la comptabilité. On retire donc
     * <b>l'identité</b>, pas <b>l'historique</b> :
     * <ol>
     *   <li>les favoris, purement personnels, sont supprimés ;</li>
     *   <li>le contenu de ses messages est effacé (il peut contenir des données personnelles) ;</li>
     *   <li>le compte est anonymisé en place : nom, prénom, e-mail, téléphone et photo sont remplacés,
     *       le mot de passe devient une valeur aléatoire — toute connexion future est impossible ;</li>
     *   <li>les rendez-vous, paiements et lignes d'audit subsistent, rattachés à ce compte anonyme.</li>
     * </ol>
     */
    @Transactional
    public void desinscrire(Integer id, String ip) {
        Utilisateur utilisateur = charger(id);
        if (!(utilisateur instanceof Membre membre)) {
            throw new OperationInterditeException("Seul un membre peut se désinscrire lui-même ; les comptes agent et "
                    + "administrateur sont gérés par l'administrateur");
        }
        // La trace d'audit est écrite AVANT l'anonymisation : elle référence le compte, pas l'identité.
        audit.enregistrer(membre, "desinscription", "utilisateur#" + id, ip);

        favoris.deleteByIdMembreId(id);                                              // 1. favoris supprimés
        for (Message message : messages.findByMembreId(id)) {                          // 2. messages vidés
            message.setContenu("[Contenu supprimé à la demande du membre]");
        }
        membre.setNom("Supprimé");                                                     // 3. identité neutralisée
        membre.setPrenom("Membre");
        membre.setEmail("supprime-" + id + "@anonyme.immoconnect.be");
        membre.setTelephone(null);
        membre.setPhotoUrl(null);
        membre.setMotDePasse(encodeur.encode(UUID.randomUUID().toString()));
        // 4. rendez-vous, paiements et audit : conservés, désormais sans identité
    }

    private Utilisateur charger(Integer id) {
        return utilisateurs.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", id));
    }
}
