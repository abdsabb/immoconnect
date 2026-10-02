package be.immoconnect.services;

import be.immoconnect.dto.BienGestion;
import be.immoconnect.dto.BienGestion.Indicateurs;
import be.immoconnect.dto.RequeteBien;
import be.immoconnect.entities.Administrateur;
import be.immoconnect.entities.AgentImmobilier;
import be.immoconnect.entities.Bien;
import be.immoconnect.entities.Photo;
import be.immoconnect.entities.StatutBien;
import be.immoconnect.entities.StatutRendezVous;
import be.immoconnect.entities.TypeOffre;
import be.immoconnect.entities.Utilisateur;
import be.immoconnect.exceptions.DonneeInvalideException;
import be.immoconnect.exceptions.OperationInterditeException;
import be.immoconnect.exceptions.RegleAnnonceException;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.repositories.AdministrateurRepository;
import be.immoconnect.repositories.AgentImmobilierRepository;
import be.immoconnect.repositories.BienRepository;
import be.immoconnect.repositories.CategorieRepository;
import be.immoconnect.repositories.FavoriRepository;
import be.immoconnect.repositories.PhotoRepository;
import be.immoconnect.repositories.RendezVousRepository;
import be.immoconnect.stockage.ImagesBiens;
import be.immoconnect.stockage.StockagePhotos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Back-office de l'agent : publier (AG1), modifier (AG2), archiver (AG3) ses annonces et consulter
 * leur tableau de bord (AG6). Chaque opération vérifie que l'annonce appartient à l'agent connecté :
 * le rôle agent ne donne aucun droit sur les annonces d'un collègue.
 */
@Service
public class ServiceAnnonces {

    /** La position d'une photo tient dans un TINYINT et la galerie reste lisible. */
    private static final int PHOTOS_MAX = 20;

    private final BienRepository biens;
    private final PhotoRepository photos;
    private final CategorieRepository categories;
    private final AgentImmobilierRepository agents;
    private final FavoriRepository favoris;
    private final RendezVousRepository rendezVous;
    private final ImagesBiens images;
    private final StockagePhotos stockage;
    private final ServiceAudit audit;
    private final Clock horloge;
    private final AdministrateurRepository administrateurs;
    private final AccesAdministrateur acces;

    public ServiceAnnonces(BienRepository biens, PhotoRepository photos, CategorieRepository categories,
                           AgentImmobilierRepository agents, FavoriRepository favoris, RendezVousRepository rendezVous,
                           ImagesBiens images, StockagePhotos stockage, ServiceAudit audit, Clock horloge,
                           AdministrateurRepository administrateurs, AccesAdministrateur acces) {
        this.administrateurs = administrateurs;
        this.acces = acces;
        this.biens = biens;
        this.photos = photos;
        this.categories = categories;
        this.agents = agents;
        this.favoris = favoris;
        this.rendezVous = rendezVous;
        this.images = images;
        this.stockage = stockage;
        this.audit = audit;
        this.horloge = horloge;
    }

    /** Toutes les annonces de l'agent, hors ligne comprises, avec leurs indicateurs. */
    @Transactional(readOnly = true)
    public List<BienGestion> mesAnnonces(Integer agentId) {
        Map<Integer, Indicateurs> indicateurs = indicateurs(agentId);
        return biens.findByAgentIdOrderByPublieLeDescIdDesc(agentId).stream()
                .map(bien -> BienGestion.depuis(bien, indicateurs.getOrDefault(bien.getId(), Indicateurs.sansActivite(bien))))
                .toList();
    }

    /**
     * Supervision : les annonces de tous les agents, ou d'un seul, pour l'administrateur gestionnaire — l'agence
     * est une structure privée, sa direction voit et corrige tout ce que publient ses agents.
     */
    @Transactional(readOnly = true)
    public List<BienGestion> toutes(Integer administrateurId, Integer agentId) {
        acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);
        List<AgentImmobilier> concernes = agentId == null ? agents.findAll()
                : List.of(agents.findById(agentId).orElseThrow(() -> new RessourceIntrouvableException("Agent", agentId)));
        return concernes.stream().flatMap(agent -> mesAnnonces(agent.getId()).stream()).toList();
    }

    @Transactional(readOnly = true)
    public BienGestion annonce(Integer agentId, Integer bienId) {
        return vue(monAnnonce(agentId, bienId));
    }

    @Transactional
    public BienGestion creer(Integer agentId, RequeteBien requete, String ip) {
        AgentImmobilier agent = agents.findById(agentId)
                .orElseThrow(() -> new OperationInterditeException("Seul un agent peut publier une annonce"));
        Bien bien = new Bien(agent);
        remplir(bien, requete);
        biens.save(bien);
        audit.enregistrer(agent, "creation_bien", "bien#" + bien.getId(), ip);
        return vue(bien);
    }

    @Transactional
    public BienGestion modifier(Integer agentId, Integer bienId, RequeteBien requete, String ip) {
        Bien bien = monAnnonce(agentId, bienId);
        remplir(bien, requete);
        if (requete.statut() != null && requete.statut() != bien.getStatut()) {
            changerStatut(bien, requete.statut());
        }
        verifierOffre(bien);
        audit.enregistrer(auteur(agentId, bien), "modification_bien", "bien#" + bienId, ip);
        return vue(bien);
    }

    /** AG3 : l'annonce quitte la recherche mais reste dans l'historique du back-office — rien n'est effacé. */
    @Transactional
    public void archiver(Integer agentId, Integer bienId, String ip) {
        Bien bien = monAnnonce(agentId, bienId);
        if (bien.getStatut() != StatutBien.archive) {
            changerStatut(bien, StatutBien.archive);
            audit.enregistrer(auteur(agentId, bien), "archivage_bien", "bien#" + bienId, ip);
        }
    }

    /**
     * Suppression définitive : l'annonce, ses photos (fichiers compris) et ses favoris disparaissent (RA10).
     * Une annonce qui a un historique de visites ne se supprime pas — les rendez-vous et les paiements doivent
     * rester lisibles : elle se met hors ligne.
     */
    @Transactional
    public void supprimer(Integer agentId, Integer bienId, String ip) {
        Bien bien = monAnnonce(agentId, bienId);
        if (rendezVous.existsByBienId(bienId)) {
            throw new RegleAnnonceException("Cette annonce a des visites dans son historique : elle ne peut pas être supprimée. Mettez-la hors ligne.");
        }
        List<String> fichiers = bien.getPhotos().stream().map(Photo::getUrl).toList();
        Utilisateur auteur = auteur(agentId, bien);
        biens.delete(bien);
        biens.flush();
        fichiers.forEach(stockage::supprimer);
        audit.enregistrer(auteur, "suppression_bien", "bien#" + bienId, ip);
    }

    @Transactional
    public BienGestion ajouterPhoto(Integer agentId, Integer bienId, byte[] fichier, String legende, String ip) {
        Bien bien = monAnnonce(agentId, bienId);
        if (bien.getPhotos().size() >= PHOTOS_MAX) {
            throw new RegleAnnonceException("Une annonce compte " + PHOTOS_MAX + " photos au plus");
        }
        String legendeNettoyee = legende == null || legende.isBlank() ? null : legende.trim();
        if (legendeNettoyee != null && legendeNettoyee.length() > 150) {
            throw new DonneeInvalideException("legende", "la légende ne peut dépasser 150 caractères");
        }
        byte[] jpeg = images.normaliser(fichier);
        String url = stockage.enregistrer(bienId, jpeg);
        // Composition : la photo est enregistrée avec son bien (cascade)
        bien.getPhotos().add(new Photo(bien, url, bien.getPhotos().size() + 1, legendeNettoyee));
        biens.flush();
        audit.enregistrer(auteur(agentId, bien), "ajout_photo", "bien#" + bienId, ip);
        return vue(bien);
    }

    /** RA6 : la dernière photo d'une annonce en ligne ne se supprime pas ; il faut d'abord la mettre hors ligne. */
    @Transactional
    public BienGestion supprimerPhoto(Integer agentId, Integer bienId, Integer photoId, String ip) {
        Bien bien = monAnnonce(agentId, bienId);
        Photo photo = photoDe(bien, photoId);
        if (bien.getPhotos().size() == 1 && estEnLigne(bien.getStatut())) {
            throw new RegleAnnonceException("Une annonce en ligne garde au moins une photo (RA6)");
        }
        List<Integer> restantes = bien.getPhotos().stream().map(Photo::getId).filter(id -> !id.equals(photoId)).toList();
        String url = photo.getUrl();
        // Composition : retirée de la galerie, la photo est supprimée (orphanRemoval)
        bien.getPhotos().remove(photo);
        biens.flush();
        renumeroter(bienId, restantes);
        stockage.supprimer(url);
        audit.enregistrer(auteur(agentId, bien), "suppression_photo", "bien#" + bienId, ip);
        return vue(monAnnonce(agentId, bienId));
    }

    /** La photo choisie passe en première position : elle devient la couverture de l'annonce. */
    @Transactional
    public BienGestion definirCouverture(Integer agentId, Integer bienId, Integer photoId, String ip) {
        Bien bien = monAnnonce(agentId, bienId);
        photoDe(bien, photoId);
        List<Integer> ordre = new ArrayList<>();
        ordre.add(photoId);
        bien.getPhotos().stream().map(Photo::getId).filter(id -> !id.equals(photoId)).forEach(ordre::add);
        renumeroter(bienId, ordre);
        audit.enregistrer(auteur(agentId, bien), "modification_bien", "bien#" + bienId, ip);
        return vue(monAnnonce(agentId, bienId));
    }

    // ---------- Règles ----------

    private void changerStatut(Bien bien, StatutBien nouveau) {
        if (estEnLigne(nouveau) && !bien.estPubliable()) {
            throw new RegleAnnonceException("Une annonce ne peut être publiée sans photo (RA6)");
        }
        // Vendu, loué ou archivé, le bien ne se visite plus : les visites prévues doivent d'abord être annulées
        if (!estEnLigne(nouveau) && rendezVous.existsByBienIdAndStatutInAndDateHeureAfter(
                bien.getId(), RendezVousRepository.ACTIFS, maintenant())) {
            throw new RegleAnnonceException("Des visites sont encore prévues pour ce bien : annulez-les avant de le retirer");
        }
        if (bien.getStatut() == StatutBien.archive && nouveau == StatutBien.disponible) {
            bien.setPublieLe(LocalDate.now(horloge));
        }
        bien.setStatut(nouveau);
    }

    /** Le statut final suit le type d'offre : ni vente « louée », ni location « vendue ». */
    private static void verifierOffre(Bien bien) {
        if (!bien.getTypeOffre().accepte(bien.getStatut())) {
            throw new RegleAnnonceException(bien.getTypeOffre() == TypeOffre.vente
                    ? "Un bien à vendre ne peut pas être marqué « loué »"
                    : "Un bien à louer ne peut pas être marqué « vendu »");
        }
    }

    private static boolean estEnLigne(StatutBien statut) {
        return statut == StatutBien.disponible || statut == StatutBien.sous_option;
    }

    private void remplir(Bien bien, RequeteBien requete) {
        bien.setCategorie(categories.findById(requete.categorieId())
                .orElseThrow(() -> new DonneeInvalideException("categorieId", "catégorie inconnue")));
        bien.setTitre(requete.titre().trim());
        bien.setDescription(requete.description().trim());
        if (requete.typeOffre() != null) {
            bien.setTypeOffre(requete.typeOffre());
        }
        bien.setPrix(requete.prix());
        bien.setSuperficie(requete.superficie());
        bien.setNbChambres(requete.nbChambres());
        bien.setPeb(requete.peb());
        bien.setAdresse(requete.adresse().trim());
        bien.setVille(requete.ville().trim());
        bien.setCodePostal(requete.codePostal());
        bien.setLatitude(requete.latitude());
        bien.setLongitude(requete.longitude());
    }

    private void renumeroter(Integer bienId, List<Integer> photosDansLOrdre) {
        if (photosDansLOrdre.isEmpty()) {
            return;
        }
        photos.decalerPositions(bienId);
        for (int rang = 0; rang < photosDansLOrdre.size(); rang++) {
            photos.fixerPosition(photosDansLOrdre.get(rang), rang + 1);
        }
    }

    /** Contrôle de propriété : l'annonce d'un autre agent est refusée (403), pas seulement masquée. */
    /** L'annonce, pour son agent responsable ou pour un administrateur gestionnaire ; tout autre compte est refusé. */
    private Bien monAnnonce(Integer agentId, Integer bienId) {
        Bien bien = biens.findWithDetailsById(bienId).orElseThrow(() -> new RessourceIntrouvableException("Bien", bienId));
        if (!bien.getAgent().getId().equals(agentId) && gestionnaire(agentId) == null) {
            throw new OperationInterditeException("Cette annonce appartient à un autre agent");
        }
        return bien;
    }

    private Administrateur gestionnaire(Integer utilisateurId) {
        return administrateurs.findById(utilisateurId)
                .filter(a -> a.isActif() && a.getNiveauAcces() >= AccesAdministrateur.GESTIONNAIRE).orElse(null);
    }

    /** Qui a agi, pour le journal d'audit (RA13) : l'agent responsable, ou l'administrateur qui est intervenu. */
    private Utilisateur auteur(Integer utilisateurId, Bien bien) {
        Administrateur administrateur = bien.getAgent().getId().equals(utilisateurId) ? null : gestionnaire(utilisateurId);
        return administrateur != null ? administrateur : bien.getAgent();
    }

    private static Photo photoDe(Bien bien, Integer photoId) {
        return bien.getPhotos().stream().filter(p -> p.getId().equals(photoId)).findFirst()
                .orElseThrow(() -> new RessourceIntrouvableException("Photo", photoId));
    }

    private BienGestion vue(Bien bien) {
        return BienGestion.depuis(bien, indicateurs(bien.getAgent().getId()).getOrDefault(bien.getId(), Indicateurs.sansActivite(bien)));
    }

    /**
     * Indicateurs du tableau de bord, par bien : les vues sont portées par le bien lui-même, favoris et
     * rendez-vous viennent de deux requêtes groupées pour toutes les annonces de l'agent.
     */
    private Map<Integer, Indicateurs> indicateurs(Integer agentId) {
        Map<Integer, long[]> compteurs = new HashMap<>();
        for (Bien bien : biens.findByAgentIdOrderByPublieLeDescIdDesc(agentId)) {
            compteurs.computeIfAbsent(bien.getId(), id -> new long[4])[0] = bien.getNbVues();
        }
        for (Object[] ligne : favoris.compterParBien(agentId)) {
            compteurs.computeIfAbsent((Integer) ligne[0], id -> new long[4])[1] = (Long) ligne[1];
        }
        for (Object[] ligne : rendezVous.compterAVenirParBien(agentId, RendezVousRepository.ACTIFS, maintenant())) {
            int rang = ligne[1] == StatutRendezVous.demande ? 2 : 3;
            compteurs.computeIfAbsent((Integer) ligne[0], id -> new long[4])[rang] = (Long) ligne[2];
        }
        Map<Integer, Indicateurs> indicateurs = new HashMap<>();
        compteurs.forEach((bienId, c) -> indicateurs.put(bienId, new Indicateurs(c[0], c[1], c[2], c[3])));
        return indicateurs;
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge);
    }
}
