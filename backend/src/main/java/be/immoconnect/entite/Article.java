package be.immoconnect.entite;

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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Article du blog rédigé par un administrateur — table article. */
@Entity
@Table(name = "article")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administrateur_id", nullable = false)
    private Administrateur administrateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categorie_article_id", nullable = false)
    private CategorieArticle categorie;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('brouillon','publie','archive')")
    private StatutArticle statut = StatutArticle.brouillon;

    @Column(name = "publie_le")
    private LocalDate publieLe;

    /** Un article naît à l'état de brouillon, invisible du public. */
    public Article(Administrateur administrateur, CategorieArticle categorie, String titre, String contenu) {
        this.administrateur = administrateur;
        this.categorie = categorie;
        this.titre = titre;
        this.contenu = contenu;
    }

    /** RA4 : un article n'est visible que s'il est publié ; publie_le est alors renseigné automatiquement. */
    public void publier(LocalDate aujourdHui) {
        this.statut = StatutArticle.publie;
        this.publieLe = aujourdHui;
    }

    /** L'article quitte le blog public ; sa date de publication est conservée pour l'historique. */
    public void archiver() {
        this.statut = StatutArticle.archive;
    }

    public boolean estPublie() {
        return statut == StatutArticle.publie;
    }
}
