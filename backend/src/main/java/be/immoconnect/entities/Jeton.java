package be.immoconnect.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Jeton à usage unique — table jeton. La valeur remise à l'utilisateur n'est jamais enregistrée :
 * seule son empreinte SHA-256 l'est, comme pour une clé API. Une base volée ne livre donc aucun
 * jeton utilisable.
 */
@Entity
@Table(name = "jeton")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Jeton {

    /** Au-delà, le défi est perdu : six chiffres ne résistent pas à des essais sans limite. */
    public static final int ESSAIS_MAX = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('rafraichissement','activation','reinitialisation','double_facteur')")
    private TypeJeton type;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, unique = true, length = 64)
    private String empreinte;

    /** Empreinte du code à six chiffres d'un double facteur ; absente des autres jetons. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "empreinte_code", length = 64)
    private String empreinteCode;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    private Integer essais = 0;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @Column(name = "expire_le", nullable = false)
    private LocalDateTime expireLe;

    @Column(name = "utilise_le")
    private LocalDateTime utiliseLe;

    @Column(nullable = false, length = 45)
    private String ip;

    public Jeton(Utilisateur utilisateur, TypeJeton type, String empreinte, String empreinteCode,
                 LocalDateTime creeLe, LocalDateTime expireLe, String ip) {
        this.utilisateur = utilisateur;
        this.type = type;
        this.empreinte = empreinte;
        this.empreinteCode = empreinteCode;
        this.creeLe = creeLe;
        this.expireLe = expireLe;
        this.ip = ip == null ? "inconnue" : ip;
    }

    public boolean estUtilisable(LocalDateTime maintenant) {
        return utiliseLe == null && expireLe.isAfter(maintenant) && essais < ESSAIS_MAX;
    }

    public boolean dejaUtilise() {
        return utiliseLe != null;
    }

    /** Un jeton ne sert qu'une fois : l'utiliser le ferme. */
    public void utiliser(LocalDateTime maintenant) {
        this.utiliseLe = maintenant;
    }

    public void compterEssai() {
        this.essais = essais + 1;
    }
}
