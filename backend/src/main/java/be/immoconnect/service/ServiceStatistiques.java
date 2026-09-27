package be.immoconnect.service;

import be.immoconnect.api.admin.Statistiques;
import be.immoconnect.entite.StatutBien;
import be.immoconnect.entite.StatutPaiement;
import be.immoconnect.entite.StatutRendezVous;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cas d'utilisation « Consulter les statistiques du site » (A8) : des agrégats, aucune donnée personnelle. */
@Service
public class ServiceStatistiques {

    private static final int COMMUNES_AFFICHEES = 8;

    private final EntityManager em;
    private final AccesAdministrateur acces;
    private final Clock horloge;

    public ServiceStatistiques(EntityManager em, AccesAdministrateur acces, Clock horloge) {
        this.em = em;
        this.acces = acces;
        this.horloge = horloge;
    }

    @Transactional(readOnly = true)
    public Statistiques calculer(Integer administrateurId) {
        acces.exiger(administrateurId, AccesAdministrateur.GESTIONNAIRE);

        Map<String, Long> biens = new LinkedHashMap<>();
        for (StatutBien statut : StatutBien.values()) {
            biens.put(statut.name(), 0L);
        }
        lignes("select b.statut, count(b) from Bien b group by b.statut").forEach(l -> biens.put(((StatutBien) l[0]).name(), (Long) l[1]));

        Map<String, Long> rendezVous = new LinkedHashMap<>();
        for (StatutRendezVous statut : StatutRendezVous.values()) {
            rendezVous.put(statut.name(), 0L);
        }
        lignes("select r.statut, count(r) from RendezVous r group by r.statut")
                .forEach(l -> rendezVous.put(((StatutRendezVous) l[0]).name(), (Long) l[1]));

        Map<String, Long> comptes = new LinkedHashMap<>();
        comptes.put("membre", compter("select count(m) from Membre m"));
        comptes.put("agent", compter("select count(a) from AgentImmobilier a"));
        comptes.put("admin", compter("select count(a) from Administrateur a"));

        long visitesAVenir = em.createQuery(
                        "select count(r) from RendezVous r where r.statut in :actifs and r.dateHeure > :maintenant", Long.class)
                .setParameter("actifs", List.of(StatutRendezVous.demande, StatutRendezVous.confirme))
                .setParameter("maintenant", LocalDateTime.now(horloge))
                .getSingleResult();

        List<Statistiques.Commune> communes = em.createQuery(
                        "select b.ville, count(b), avg(b.prix) from Bien b where b.statut = :disponible "
                                + "group by b.ville order by count(b) desc, b.ville", Object[].class)
                .setParameter("disponible", StatutBien.disponible)
                .setMaxResults(COMMUNES_AFFICHEES)
                .getResultList().stream()
                .map(l -> new Statistiques.Commune((String) l[0], (Long) l[1],
                        BigDecimal.valueOf(((Number) l[2]).doubleValue()).setScale(0, RoundingMode.HALF_UP)))
                .toList();

        return new Statistiques(biens, comptes,
                compter("select count(u) from Utilisateur u where u.actif = false"),
                rendezVous, visitesAVenir,
                compterPaiements(StatutPaiement.reussi), somme(StatutPaiement.reussi), somme(StatutPaiement.rembourse),
                compter("select count(f) from Favori f"), compter("select count(m) from Message m"), communes);
    }

    private List<Object[]> lignes(String jpql) {
        return em.createQuery(jpql, Object[].class).getResultList();
    }

    private long compter(String jpql) {
        return em.createQuery(jpql, Long.class).getSingleResult();
    }

    private long compterPaiements(StatutPaiement statut) {
        return em.createQuery("select count(p) from Paiement p where p.statut = :statut", Long.class)
                .setParameter("statut", statut).getSingleResult();
    }

    private BigDecimal somme(StatutPaiement statut) {
        return em.createQuery("select coalesce(sum(p.montant), 0) from Paiement p where p.statut = :statut", BigDecimal.class)
                .setParameter("statut", statut).getSingleResult();
    }
}
