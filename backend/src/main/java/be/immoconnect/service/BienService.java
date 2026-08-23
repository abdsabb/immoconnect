package be.immoconnect.service;

import be.immoconnect.api.bien.BienDetail;
import be.immoconnect.api.bien.BienResume;
import be.immoconnect.api.bien.CritereRechercheBien;
import be.immoconnect.depot.BienRepository;
import be.immoconnect.depot.BienSpecifications;
import be.immoconnect.entite.Bien;
import be.immoconnect.entite.StatutBien;
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
        Specification<Bien> specification = Specification.allOf(
                BienSpecifications.statut(criteres.statutEffectif()),
                BienSpecifications.ville(criteres.ville()),
                BienSpecifications.categorie(criteres.categorieId()),
                BienSpecifications.prixMin(criteres.prixMin()),
                BienSpecifications.prixMax(criteres.prixMax()),
                BienSpecifications.chambresMin(criteres.chambresMin()),
                BienSpecifications.superficieMin(criteres.superficieMin()));
        return biens.findAll(specification, pagination).map(BienResume::depuis);
    }

    /** Un bien archivé n'est plus visible publiquement : il est traité comme introuvable (RA5, livrable 15). */
    public BienDetail detail(Integer id) {
        Bien bien = biens.findWithDetailsById(id)
                .filter(b -> b.getStatut() != StatutBien.archive)
                .orElseThrow(() -> new RessourceIntrouvableException("Bien", id));
        return BienDetail.depuis(bien);
    }
}
