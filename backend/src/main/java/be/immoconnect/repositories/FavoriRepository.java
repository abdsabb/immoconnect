package be.immoconnect.repositories;

import be.immoconnect.entities.Bien;
import be.immoconnect.entities.Favori;
import be.immoconnect.entities.FavoriId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FavoriRepository extends JpaRepository<Favori, FavoriId> {

    long deleteByIdMembreId(Integer membreId);

    /** Tous les favoris d'un membre, pour l'export de ses données. */
    @EntityGraph(attributePaths = {"bien"})
    List<Favori> findByIdMembreIdOrderByDateAjoutDesc(Integer membreId);

    /** Les biens favoris d'un membre, du plus récemment ajouté au plus ancien. */
    @Query(value = "select f.bien from Favori f where f.id.membreId = :membreId order by f.dateAjout desc",
            countQuery = "select count(f) from Favori f where f.id.membreId = :membreId")
    Page<Bien> biensFavoris(Integer membreId, Pageable pagination);

    /** Nombre de favoris par bien d'un agent : lignes [identifiant du bien, nombre]. */
    @Query("select f.id.bienId, count(f) from Favori f where f.bien.agent.id = :agentId group by f.id.bienId")
    List<Object[]> compterParBien(Integer agentId);
}
