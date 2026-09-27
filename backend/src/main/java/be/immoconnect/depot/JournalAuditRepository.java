package be.immoconnect.depot;

import be.immoconnect.entite.JournalAudit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface JournalAuditRepository extends JpaRepository<JournalAudit, Integer>, JpaSpecificationExecutor<JournalAudit> {

    /** Les actions présentes dans le journal, pour le filtre du back-office. */
    @Query("select distinct j.action from JournalAudit j order by j.action")
    List<String> actions();
}
