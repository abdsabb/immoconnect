package be.immoconnect.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Alerte de la détection d'intrusion — table alerte_securite. Elle décrit une adresse IP et ce qui l'a
 * rendue suspecte, pas un compte : elle vit donc à part du journal d'audit, dont chaque ligne a un auteur.
 */
@Entity
@Table(name = "alerte_securite")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlerteSecurite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('rafale_echecs','enumeration','cle_revoquee')")
    private TypeAlerte type;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(nullable = false)
    private String detail;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    public AlerteSecurite(TypeAlerte type, String ip, String detail, LocalDateTime creeLe) {
        this.type = type;
        this.ip = ip;
        this.detail = detail;
        this.creeLe = creeLe;
    }
}
