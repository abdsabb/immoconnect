package be.immoconnect.services;

import be.immoconnect.dto.RequeteDecision;
import be.immoconnect.dto.RequeteSignalement;
import be.immoconnect.dto.SignalementResume;
import be.immoconnect.entities.Administrateur;
import be.immoconnect.entities.Article;
import be.immoconnect.entities.Bien;
import be.immoconnect.entities.Message;
import be.immoconnect.entities.Signalement;
import be.immoconnect.entities.StatutBien;
import be.immoconnect.entities.Utilisateur;
import be.immoconnect.exceptions.DonneeInvalideException;
import be.immoconnect.exceptions.OperationInterditeException;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.repositories.ArticleRepository;
import be.immoconnect.repositories.BienRepository;
import be.immoconnect.repositories.MessageRepository;
import be.immoconnect.repositories.SignalementRepository;
import be.immoconnect.repositories.UtilisateurRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signalement de contenus (chapitre 11 du rapport, règlement européen sur les services numériques) :
 * tout utilisateur connecté peut signaler un message, une annonce ou un article ; un administrateur
 * gestionnaire tranche, retire ou conserve, et motive sa décision, qui est journalisée.
 */
@Service
public class ServiceSignalements {

    /** Texte qui remplace un message retiré : l'échange garde sa trace, pas son contenu. */
    public static final String MESSAGE_RETIRE = "[Message retiré par la modération]";
    private static final int APERCU = 160;

    private final SignalementRepository signalements;
    private final UtilisateurRepository utilisateurs;
    private final MessageRepository messages;
    private final BienRepository biens;
    private final ArticleRepository articles;
    private final AccesAdministrateur acces;
    private final ServiceAudit audit;
    private final Clock horloge;

    public ServiceSignalements(SignalementRepository signalements, UtilisateurRepository utilisateurs, MessageRepository messages,
                               BienRepository biens, ArticleRepository articles, AccesAdministrateur acces, ServiceAudit audit,
                               Clock horloge) {
        this.signalements = signalements;
        this.utilisateurs = utilisateurs;
        this.messages = messages;
        this.biens = biens;
        this.articles = articles;
        this.acces = acces;
        this.audit = audit;
        this.horloge = horloge;
    }

    @Transactional
    public SignalementResume signaler(Integer utilisateurId, String role, RequeteSignalement requete, String ip) {
        Utilisateur auteur = utilisateurs.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", utilisateurId));
        String apercu = apercu(requete.typeContenu(), requete.contenuId());
        if (requete.typeContenu() == Signalement.TypeContenu.message && !peutVoirLeMessage(utilisateurId, role, requete.contenuId())) {
            throw new OperationInterditeException("On ne signale qu'un message que l'on a reçu");
        }
        if (signalements.existsByAuteurIdAndTypeContenuAndContenuIdAndStatut(utilisateurId, requete.typeContenu(),
                requete.contenuId(), Signalement.Statut.ouvert)) {
            throw new DonneeInvalideException("contenuId", "vous avez déjà signalé ce contenu ; il est en cours d'examen");
        }
        Signalement signalement = signalements.save(new Signalement(auteur, requete.typeContenu(), requete.contenuId(),
                requete.motif(), requete.description().trim(), LocalDateTime.now(horloge)));
        audit.enregistrer(auteur, "signalement", "signalement#" + signalement.getId(), ip);
        return SignalementResume.depuis(signalement, apercu);
    }

    @Transactional(readOnly = true)
    public Page<SignalementResume> lister(Integer administrateurId, Signalement.Statut statut, Pageable pagination) {
        acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        Page<Signalement> page = statut == null ? signalements.findAllByOrderByCreeLeDescIdDesc(pagination)
                : signalements.findByStatutOrderByCreeLeDescIdDesc(statut, pagination);
        return page.map(s -> SignalementResume.depuis(s, apercuSansErreur(s)));
    }

    @Transactional(readOnly = true)
    public long ouverts(Integer administrateurId) {
        acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        return signalements.countByStatut(Signalement.Statut.ouvert);
    }

    /**
     * Décision de l'administrateur. « retire » agit sur le contenu : un message perd son texte, une annonce
     * est mise hors ligne, un article est archivé. « conserve » le laisse en place. Dans les deux cas, la
     * motivation est conservée et l'action journalisée (RA13).
     */
    @Transactional
    public SignalementResume trancher(Integer administrateurId, Integer id, RequeteDecision requete, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        Signalement signalement = signalements.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Signalement", id));
        try {
            signalement.trancher(requete.statut(), requete.decision().trim(), administrateur, LocalDateTime.now(horloge));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new DonneeInvalideException("statut", e.getMessage());
        }
        if (requete.statut() == Signalement.Statut.retire) {
            retirer(signalement);
        }
        audit.enregistrer(administrateur, requete.statut() == Signalement.Statut.retire ? "retrait_contenu" : "conservation_contenu",
                "signalement#" + id, ip);
        return SignalementResume.depuis(signalement, apercuSansErreur(signalement));
    }

    private void retirer(Signalement s) {
        switch (s.getTypeContenu()) {
            case message -> messages.findById(s.getContenuId()).ifPresent(m -> m.retirer(MESSAGE_RETIRE));
            case bien -> biens.findById(s.getContenuId()).ifPresent(b -> b.setStatut(StatutBien.archive));
            case article -> articles.findById(s.getContenuId()).ifPresent(a -> { if (a.estPublie()) a.archiver(); });
        }
    }

    /** Le contenu doit exister : on ne signale pas dans le vide. */
    private String apercu(Signalement.TypeContenu type, Integer contenuId) {
        return switch (type) {
            case message -> messages.findById(contenuId).map(Message::getContenu)
                    .orElseThrow(() -> new RessourceIntrouvableException("Message", contenuId));
            case bien -> biens.findById(contenuId).map(Bien::getTitre)
                    .orElseThrow(() -> new RessourceIntrouvableException("Bien", contenuId));
            case article -> articles.findById(contenuId).map(Article::getTitre)
                    .orElseThrow(() -> new RessourceIntrouvableException("Article", contenuId));
        };
    }

    private String apercuSansErreur(Signalement s) {
        try {
            String texte = apercu(s.getTypeContenu(), s.getContenuId());
            return texte.length() > APERCU ? texte.substring(0, APERCU) + "…" : texte;
        } catch (RessourceIntrouvableException e) {
            return null;
        }
    }

    private boolean peutVoirLeMessage(Integer utilisateurId, String role, Integer messageId) {
        return messages.findById(messageId)
                .map(m -> m.getDestinataire().getId().equals(utilisateurId) || "admin".equals(role))
                .orElse(false);
    }
}
