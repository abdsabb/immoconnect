package be.immoconnect.depot;

import be.immoconnect.entite.Bien;
import be.immoconnect.entite.Favori;
import be.immoconnect.entite.FavoriId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FavoriRepository extends JpaRepository<Favori, FavoriId> {

    long deleteByIdMembreId(Integer membreId);

    /** Les biens favoris d'un membre, du plus récemment ajouté au plus ancien. */
    @Query(value = "select f.bien from Favori f where f.id.membreId = :membreId order by f.dateAjout desc",
            countQuery = "select count(f) from Favori f where f.id.membreId = :membreId")
    Page<Bien> biensFavoris(Integer membreId, Pageable pagination);

    /** Nombre de favoris par bien d'un agent : lignes [identifiant du bien, nombre]. */
    @Query("select f.id.bienId, count(f) from Favori f where f.bien.agent.id = :agentId group by f.id.bienId")
    List<Object[]> compterParBien(Integer agentId);
}
