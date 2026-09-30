package be.immoconnect.services;

import be.immoconnect.dto.BienResume;
import be.immoconnect.entities.Bien;
import be.immoconnect.entities.Favori;
import be.immoconnect.entities.FavoriId;
import be.immoconnect.entities.Membre;
import be.immoconnect.entities.StatutBien;
import be.immoconnect.exceptions.BienIndisponibleException;
import be.immoconnect.exceptions.OperationInterditeException;
import be.immoconnect.exceptions.RessourceIntrouvableException;
import be.immoconnect.repositories.BienRepository;
import be.immoconnect.repositories.FavoriRepository;
import be.immoconnect.repositories.MembreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cas d'utilisation « Ajouter un bien à ses favoris » (M1) et « Consulter sa liste de favoris » (M2). */
@Service
public class ServiceFavori {

    private final FavoriRepository favoris;
    private final BienRepository biens;
    private final MembreRepository membres;

    public ServiceFavori(FavoriRepository favoris, BienRepository biens, MembreRepository membres) {
        this.favoris = favoris;
        this.biens = biens;
        this.membres = membres;
    }

    /** Un bien déjà vendu ou loué reste dans la liste : le membre voit ce qu'il est devenu. */
    @Transactional(readOnly = true)
    public Page<BienResume> lister(Integer membreId, Pageable pagination) {
        return favoris.biensFavoris(membreId, pagination).map(BienResume::depuis);
    }

    /** Ajouter deux fois le même bien n'est pas une erreur : PUT est idempotent. RA5 : le bien doit être disponible. */
    @Transactional
    public void ajouter(Integer membreId, Integer bienId) {
        Membre membre = membres.findById(membreId)
                .orElseThrow(() -> new OperationInterditeException("Seul un membre peut avoir des favoris"));
        Bien bien = biens.findById(bienId)
                .filter(b -> b.getStatut() != StatutBien.archive)
                .orElseThrow(() -> new RessourceIntrouvableException("Bien", bienId));
        if (favoris.existsById(new FavoriId(membreId, bienId))) {
            return;
        }
        if (!bien.estDisponible()) {
            throw new BienIndisponibleException(bienId);
        }
        favoris.save(new Favori(membre, bien));
    }

    @Transactional
    public void retirer(Integer membreId, Integer bienId) {
        FavoriId id = new FavoriId(membreId, bienId);
        if (favoris.existsById(id)) {
            favoris.deleteById(id);
        }
    }
}
