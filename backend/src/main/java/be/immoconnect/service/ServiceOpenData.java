package be.immoconnect.service;

import be.immoconnect.depot.BienRepository;
import be.immoconnect.depot.BienSpecifications;
import be.immoconnect.entite.Bien;
import be.immoconnect.entite.StatutBien;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jeux de données ouverts (livrable 15, §7). L'anonymisation est faite ici, par construction : les
 * objets renvoyés n'ont tout simplement pas de champ pour l'adresse, l'agent ou un identifiant.
 */
@Service
@Transactional(readOnly = true)
public class ServiceOpenData {

    /** Trois décimales : une position à une centaine de mètres près, qui situe le quartier sans désigner la maison. */
    private static final int DECIMALES_POSITION = 3;
    private static final String SEPARATEUR = ";";

    public record BienOuvert(String categorie, String ville, String codePostal, BigDecimal prix, BigDecimal superficie,
                             Integer nbChambres, BigDecimal latitude, BigDecimal longitude, LocalDate publieLe) {
    }

    public record StatistiqueMarche(String commune, String categorie, long nbAnnonces, BigDecimal prixMoyen, BigDecimal prixMedianM2) {
    }

    private final BienRepository biens;

    public ServiceOpenData(BienRepository biens) {
        this.biens = biens;
    }

    public List<BienOuvert> biens() {
        return disponibles().stream().map(b -> new BienOuvert(b.getCategorie().getNom(), b.getVille(), b.getCodePostal(),
                b.getPrix(), b.getSuperficie(), b.getNbChambres(),
                b.getLatitude().setScale(DECIMALES_POSITION, RoundingMode.HALF_UP),
                b.getLongitude().setScale(DECIMALES_POSITION, RoundingMode.HALF_UP), b.getPublieLe())).toList();
    }

    public String biensCsv() {
        StringBuilder csv = new StringBuilder("categorie;ville;code_postal;prix;superficie;nb_chambres;latitude;longitude;publie_le\r\n");
        for (BienOuvert b : biens()) {
            csv.append(String.join(SEPARATEUR, texte(b.categorie()), texte(b.ville()), texte(b.codePostal()),
                    b.prix().toPlainString(), b.superficie().toPlainString(), String.valueOf(b.nbChambres()),
                    b.latitude().toPlainString(), b.longitude().toPlainString(), b.publieLe().toString())).append("\r\n");
        }
        return csv.toString();
    }

    public List<StatistiqueMarche> statistiques() {
        Map<String, List<Bien>> groupes = new TreeMap<>();
        for (Bien bien : disponibles()) {
            groupes.computeIfAbsent(bien.getVille() + "\u0000" + bien.getCategorie().getNom(), cle -> new ArrayList<>()).add(bien);
        }
        return groupes.values().stream().map(groupe -> {
            BigDecimal total = groupe.stream().map(Bien::getPrix).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<BigDecimal> prixAuM2 = groupe.stream()
                    .map(b -> b.getPrix().divide(b.getSuperficie(), 2, RoundingMode.HALF_UP))
                    .sorted(Comparator.naturalOrder()).toList();
            return new StatistiqueMarche(groupe.getFirst().getVille(), groupe.getFirst().getCategorie().getNom(), groupe.size(),
                    total.divide(BigDecimal.valueOf(groupe.size()), 0, RoundingMode.HALF_UP), mediane(prixAuM2));
        }).toList();
    }

    private List<Bien> disponibles() {
        return biens.findAll(BienSpecifications.statut(StatutBien.disponible), Sort.by("ville", "id"));
    }

    private static BigDecimal mediane(List<BigDecimal> valeursTriees) {
        int milieu = valeursTriees.size() / 2;
        BigDecimal mediane = valeursTriees.size() % 2 == 1 ? valeursTriees.get(milieu)
                : valeursTriees.get(milieu - 1).add(valeursTriees.get(milieu)).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        return mediane.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Champ texte d'un CSV : entre guillemets, guillemets doublés. Un texte qui commence par =, +, - ou @
     * serait lu comme une formule par un tableur : il est précédé d'une apostrophe (injection CSV).
     */
    private static String texte(String valeur) {
        String propre = valeur == null ? "" : valeur.replace("\"", "\"\"");
        if (!propre.isEmpty() && "=+-@".indexOf(propre.charAt(0)) >= 0) {
            propre = "'" + propre;
        }
        return "\"" + propre + "\"";
    }
}
