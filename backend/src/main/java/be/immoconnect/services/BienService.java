package be.immoconnect.services;

import be.immoconnect.dto.BienDetail;
import be.immoconnect.dto.BienResume;
import be.immoconnect.dto.CritereRechercheBien;
import be.immoconnect.entities.Bien;
import be.immoconnect.entities.StatutBien;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.repositories.BienRepository;
import be.immoconnect.repositories.BienSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cas d'utilisation du catalogue : rechercher un bien (V2) et consulter le détail d'une annonce (V4). */
@Service
@Transactional(readOnly = true)
public class BienService {

    private final BienRepository biens;

    public BienService(BienRepository biens) {
        this.biens = biens;
    }

    public Page<BienResume> rechercher(CritereRechercheBien criteres, Pageable pagination) {
        // RA5 : un bien hors ligne n'est pas public, la recherche ne doit pas permettre de le lister. Les biens
        // vendus ou loués restent visibles, avec leur statut : ils montrent l'activité de l'agence.
        if (criteres.statut() == StatutBien.archive) {
            throw new IllegalArgumentException("Statut de recherche inconnu : archive");
        }
        Specification<Bien> specification = Specification.allOf(
                BienSpecifications.statut(criteres.statut()),
                BienSpecifications.typeOffre(criteres.typeOffre()),
                BienSpecifications.ville(criteres.ville()),
                BienSpecifications.categorie(criteres.categorieId()),
                BienSpecifications.prixMin(criteres.prixMin()),
                BienSpecifications.prixMax(criteres.prixMax()),
                BienSpecifications.chambresMin(criteres.chambresMin()),
                BienSpecifications.superficieMin(criteres.superficieMin()));
        return biens.findAll(specification, pagination).map(BienResume::depuis);
    }

    /**
     * Un bien archivé n'est plus visible publiquement : il est traité comme introuvable (RA5, livrable 15).
     * Chaque consultation compte une vue pour le tableau de bord de l'agent (AG6), sauf celle de l'agent
     * ou d'un administrateur, qui regardent leur propre travail.
     */
    @Transactional
    public BienDetail detail(Integer id, boolean visiteur) {
        Bien bien = biens.findWithDetailsById(id)
                .filter(b -> b.getStatut() != StatutBien.archive)
                .orElseThrow(() -> new RessourceIntrouvableException("Bien", id));
        if (visiteur) {
            biens.compterUneVue(id);
        }
        return BienDetail.depuis(bien);
    }
}
