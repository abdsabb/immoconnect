package be.immoconnect.services;

import be.immoconnect.dto.CategorieResume;
import be.immoconnect.dto.RequeteCategorie;
import be.immoconnect.entities.Administrateur;
import be.immoconnect.entities.Categorie;
import be.immoconnect.exceptions.DonneeInvalideException;
import be.immoconnect.exceptions.RegleAnnonceException;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.repositories.BienRepository;
import be.immoconnect.repositories.CategorieRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cas d'utilisation « Gérer les catégories de biens » (A3). */
@Service
public class ServiceCategories {

    private final CategorieRepository categories;
    private final BienRepository biens;
    private final AccesAdministrateur acces;
    private final ServiceAudit audit;

    public ServiceCategories(CategorieRepository categories, BienRepository biens, AccesAdministrateur acces, ServiceAudit audit) {
        this.categories = categories;
        this.biens = biens;
        this.acces = acces;
        this.audit = audit;
    }

    /** Liste publique, par ordre alphabétique : elle sert à la recherche et au formulaire d'annonce. */
    @Transactional(readOnly = true)
    public List<CategorieResume> lister() {
        return categories.findAll(Sort.by("nom")).stream().map(CategorieResume::depuis).toList();
    }

    @Transactional
    public CategorieResume creer(Integer administrateurId, RequeteCategorie requete, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        exigerNomLibre(requete.nom(), null);
        Categorie categorie = categories.save(new Categorie(requete.nom().trim(), nettoyer(requete.description())));
        audit.enregistrer(administrateur, "creation_categorie", "categorie#" + categorie.getId(), ip);
        return CategorieResume.depuis(categorie);
    }

    @Transactional
    public CategorieResume modifier(Integer administrateurId, Integer id, RequeteCategorie requete, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        Categorie categorie = charger(id);
        exigerNomLibre(requete.nom(), id);
        categorie.setNom(requete.nom().trim());
        categorie.setDescription(nettoyer(requete.description()));
        audit.enregistrer(administrateur, "modification_categorie", "categorie#" + id, ip);
        return CategorieResume.depuis(categorie);
    }

    /** Une catégorie encore portée par des biens ne se supprime pas : la base le refuserait aussi (clé étrangère RESTRICT). */
    @Transactional
    public void supprimer(Integer administrateurId, Integer id, String ip) {
        Administrateur administrateur = acces.exiger(administrateurId, AccesAdministrateur.EDITEUR);
        Categorie categorie = charger(id);
        long utilisations = biens.countByCategorieId(id);
        if (utilisations > 0) {
            throw new RegleAnnonceException("La catégorie « " + categorie.getNom() + " » est utilisée par " + utilisations
                    + " bien(s) : elle ne peut pas être supprimée");
        }
        categories.delete(categorie);
        audit.enregistrer(administrateur, "suppression_categorie", "categorie#" + id, ip);
    }

    private void exigerNomLibre(String nom, Integer saufId) {
        categories.findByNomIgnoreCase(nom.trim())
                .filter(existante -> !existante.getId().equals(saufId))
                .ifPresent(existante -> {
                    throw new DonneeInvalideException("nom", "une catégorie porte déjà ce nom");
                });
    }

    private Categorie charger(Integer id) {
        return categories.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Catégorie", id));
    }

    private static String nettoyer(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
