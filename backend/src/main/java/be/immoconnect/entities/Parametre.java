package be.immoconnect.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Paramètre du site réglé par l'administrateur (cas A5) — table parametre. Une ligne par clé :
 * nom et coordonnées de l'agence, langues actives. Les secrets (Stripe, SMTP) n'y sont jamais :
 * ils restent dans l'environnement du serveur.
 */
@Entity
@Table(name = "parametre")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Parametre {

    @Id
    @Column(length = 60)
    private String cle;

    @Column(nullable = false, length = 1000)
    private String valeur;

    @Column(name = "modifie_le", nullable = false)
    private LocalDateTime modifieLe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifie_par")
    private Administrateur modifiePar;

    public Parametre(String cle, String valeur, Administrateur auteur, LocalDateTime maintenant) {
        this.cle = cle;
        modifier(valeur, auteur, maintenant);
    }

    public void modifier(String valeur, Administrateur auteur, LocalDateTime maintenant) {
        this.valeur = valeur;
        this.modifiePar = auteur;
        this.modifieLe = maintenant;
    }
}
