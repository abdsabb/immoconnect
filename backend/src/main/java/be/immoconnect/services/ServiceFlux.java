package be.immoconnect.services;

import be.immoconnect.config.ConfigurationHorloge;
import be.immoconnect.entities.Article;
import be.immoconnect.entities.Bien;
import be.immoconnect.entities.StatutArticle;
import be.immoconnect.entities.StatutBien;
import be.immoconnect.entities.TypeOffre;
import be.immoconnect.repositories.ArticleRepository;
import be.immoconnect.repositories.BienRepository;
import be.immoconnect.repositories.BienSpecifications;
import be.immoconnect.services.FluxRss.Canal;
import be.immoconnect.services.FluxRss.Element;
import be.immoconnect.services.FluxRss.Piece;
import be.immoconnect.stockage.StockageDisque;
import be.immoconnect.stockage.StockagePhotos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flux RSS publics : les derniers articles du blog et les dernières annonces. Un flux ne montre que
 * ce que le site montre déjà à un visiteur : articles publiés (RA4), biens disponibles (RA5), jamais
 * l'adresse exacte d'un bien ni le nom de son agent.
 */
@Service
@Transactional(readOnly = true)
public class ServiceFlux {

    public static final int ELEMENTS = 20;
    public static final String CHEMIN = "/api/v1/flux";

    private static final int LONGUEUR_RESUME = 300;
    private static final Locale BELGIQUE = Locale.of("fr", "BE");
    private static final Sort RECENTS = Sort.by(Sort.Direction.DESC, "publieLe", "id");

    private final ArticleRepository articles;
    private final BienRepository biens;
    private final Path photos;
    private final String site;
    private final Clock horloge;

    public ServiceFlux(ArticleRepository articles, BienRepository biens, StockageDisque stockage,
                       @Value("${immoconnect.courriel.url-site}") String site, Clock horloge) {
        this.articles = articles;
        this.biens = biens;
        this.photos = stockage.racine();
        this.site = site.replaceAll("/+$", "");
        this.horloge = horloge;
    }

    public String articles() {
        List<Element> elements = articles.parStatut(StatutArticle.publie, null, PageRequest.of(0, ELEMENTS, RECENTS))
                .map(this::element).getContent();
        Canal canal = new Canal("ImmoConnect — le blog", site + "/blog",
                "Conseils, marché et actualité de l'immobilier à Bruxelles et en Belgique.",
                site + CHEMIN + "/articles", maintenant());
        return FluxRss.ecrire(canal, elements);
    }

    /** @param typeOffre vente, location, ou {@code null} pour les deux */
    public String biens(TypeOffre typeOffre) {
        Specification<Bien> disponibles = Specification.allOf(
                BienSpecifications.statut(StatutBien.disponible), BienSpecifications.typeOffre(typeOffre));
        List<Element> elements = biens.findAll(disponibles, PageRequest.of(0, ELEMENTS, RECENTS))
                .map(this::element).getContent();
        String offre = typeOffre == null ? "à vendre et à louer" : libelle(typeOffre).toLowerCase(BELGIQUE);
        String page = typeOffre == null ? "/biens" : typeOffre == TypeOffre.vente ? "/a-vendre" : "/a-louer";
        String filtre = typeOffre == null ? "" : "?typeOffre=" + typeOffre;
        Canal canal = new Canal("ImmoConnect — biens " + offre, site + page,
                "Les dernières annonces de biens " + offre + ".", site + CHEMIN + "/biens" + filtre, maintenant());
        return FluxRss.ecrire(canal, elements);
    }

    private Element element(Article article) {
        return new Element(article.getTitre(), site + "/blog/" + article.getId(), resume(article.getContenu()),
                article.getCategorie().getNom(), article.getAdministrateur().getNomComplet(),
                minuit(article.getPublieLe()), null);
    }

    private Element element(Bien bien) {
        String fiche = String.join(" · ", libelle(bien.getTypeOffre()), bien.getCategorie().getNom(),
                bien.getCodePostal() + " " + bien.getVille(), nombre(bien.getSuperficie()) + " m²",
                bien.getNbChambres() + (bien.getNbChambres() > 1 ? " chambres" : " chambre"), prix(bien));
        return new Element(bien.getTitre(), site + "/biens/" + bien.getId(), fiche + ". " + resume(bien.getDescription()),
                bien.getCategorie().getNom(), null, minuit(bien.getPublieLe()), couverture(bien));
    }

    /** La photo de couverture, si son fichier existe : RSS exige la taille d'une pièce jointe. */
    private Piece couverture(Bien bien) {
        if (bien.getPhotos().isEmpty()) {
            return null;
        }
        String adresse = bien.getPhotos().getFirst().getUrl();
        if (!adresse.startsWith(StockagePhotos.PREFIXE_URL)) {
            return null;
        }
        Path fichier = photos.resolve(adresse.substring(StockagePhotos.PREFIXE_URL.length())).normalize();
        try {
            return fichier.startsWith(photos) && Files.isRegularFile(fichier)
                    ? new Piece(site + adresse, Files.size(fichier), "image/jpeg") : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static String prix(Bien bien) {
        String montant = nombre(bien.getPrix()) + " €";
        return bien.getTypeOffre() == TypeOffre.location ? montant + " par mois" : montant;
    }

    private static String nombre(Number valeur) {
        NumberFormat format = NumberFormat.getIntegerInstance(BELGIQUE);
        // Espace ordinaire entre les milliers : l'espace fine de la locale s'affiche mal dans certains lecteurs
        return format.format(valeur).replace(' ', ' ').replace(' ', ' ');
    }

    private static String libelle(TypeOffre typeOffre) {
        return typeOffre == TypeOffre.vente ? "À vendre" : "À louer";
    }

    private static String resume(String texte) {
        String propre = texte.strip().replaceAll("\\s+", " ");
        return propre.length() <= LONGUEUR_RESUME ? propre : propre.substring(0, LONGUEUR_RESUME - 1).stripTrailing() + "…";
    }

    /** Une date de publication n'a pas d'heure : le flux annonce minuit, heure de l'agence. */
    private static ZonedDateTime minuit(LocalDate jour) {
        return jour == null ? null : jour.atStartOfDay(ConfigurationHorloge.FUSEAU_AGENCE);
    }

    private ZonedDateTime maintenant() {
        return ZonedDateTime.now(horloge);
    }
}
