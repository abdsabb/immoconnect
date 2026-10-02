package be.immoconnect.notification;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mise en page HTML des e-mails, aux couleurs de la charte (bleu nuit, corail, turquoise). Le texte du modèle
 * est la source unique : cette classe le présente — titre, paragraphes, « libellé : valeur » en gras, lien
 * transformé en bouton, code de connexion mis en valeur. La version texte part avec le message, pour les
 * messageries qui n'affichent pas le HTML.
 * <p>
 * Mise en page par tableaux et styles en ligne : c'est ce que les messageries affichent de façon fiable.
 */
final class MiseEnPageCourriel {

    private static final String NUIT = "#1B3A4B";
    private static final String CORAIL = "#E76F51";
    private static final String PERLE = "#F5F7FA";
    private static final String POLICE = "font-family:'Segoe UI',Helvetica,Arial,sans-serif;";
    private static final Pattern LIEN = Pattern.compile("^https?://\\S+$");
    private static final Pattern LIBELLE = Pattern.compile("^([\\p{L}][\\p{L} ’'-]{1,28}?)( ?:) (.+)$");

    private MiseEnPageCourriel() {
    }

    /**
     * @param titre     sujet de l'e-mail, sans le préfixe « [ImmoConnect] »
     * @param corps     texte du modèle, paragraphes séparés par une ligne vide
     * @param bouton    libellé du bouton qui remplace un lien seul sur sa ligne, ou null si le modèle n'a pas de lien
     * @param code      code à mettre en valeur (connexion en deux étapes), ou null
     * @param signature texte de pied d'e-mail
     */
    static String html(String langue, String titre, String corps, String bouton, String code, String signature, String urlSite) {
        StringBuilder contenu = new StringBuilder();
        for (String paragraphe : corps.split("\\n\\s*\\n")) {
            if (paragraphe.isBlank()) {
                continue;
            }
            String ligneSeule = paragraphe.strip();
            if (code != null && ligneSeule.contains(code) && !ligneSeule.contains("\n")) {
                contenu.append(paragraphe(e(ligneSeule.replace(code, "").strip())))
                        .append("<p style=\"margin:0 0 20px;text-align:center;\"><span style=\"display:inline-block;padding:12px 24px;border-radius:8px;background:")
                        .append(PERLE).append(";color:").append(NUIT).append(";font-size:30px;font-weight:800;letter-spacing:8px;").append(POLICE).append("\">")
                        .append(e(code)).append("</span></p>");
                continue;
            }
            StringBuilder texte = new StringBuilder();
            String lien = null;
            for (String ligne : paragraphe.split("\\n")) {
                String l = ligne.strip();
                // Sans libellé de bouton, le modèle ne porte pas de lien : une adresse saisie par un visiteur reste du texte
                if (bouton != null && LIEN.matcher(l).matches()) {
                    lien = l;
                    continue;
                }
                if (texte.length() > 0) {
                    texte.append("<br>");
                }
                Matcher m = LIBELLE.matcher(l);
                texte.append(m.matches() ? "<strong>" + e(m.group(1)) + "</strong>" + m.group(2) + " " + e(m.group(3)) : e(l));
            }
            if (texte.length() > 0) {
                contenu.append(paragraphe(texte.toString()));
            }
            if (lien != null) {
                contenu.append(bouton(lien, bouton));
            }
        }
        String site = urlSite.replaceFirst("^https?://", "");
        return "<!doctype html><html lang=\"" + e(langue) + "\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>" + e(titre) + "</title></head>"
                + "<body style=\"margin:0;padding:0;background:" + PERLE + ";\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + PERLE + ";padding:24px 12px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;background:#ffffff;border-radius:12px;overflow:hidden;\">"
                + "<tr><td style=\"background:" + NUIT + ";padding:20px 32px;\"><a href=\"" + e(urlSite) + "\" style=\"text-decoration:none;font-size:24px;font-weight:800;color:#ffffff;" + POLICE + "\">"
                + "Immo<span style=\"color:" + CORAIL + ";\">Connect</span></a></td></tr>"
                + "<tr><td style=\"padding:32px 32px 12px;\"><h1 style=\"margin:0 0 20px;font-size:22px;line-height:1.3;color:" + NUIT + ";" + POLICE + "\">" + e(titre) + "</h1>"
                + contenu + "</td></tr>"
                + "<tr><td style=\"padding:20px 32px 28px;border-top:1px solid #E5E7EB;font-size:13px;line-height:1.5;color:#6B7280;" + POLICE + "\">"
                + e(signature).replace("\n", "<br>") + "<br><a href=\"" + e(urlSite) + "\" style=\"color:#2A9D8F;\">" + e(site) + "</a></td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private static String paragraphe(String html) {
        return "<p style=\"margin:0 0 16px;font-size:16px;line-height:1.6;color:#1F2933;" + POLICE + "\">" + html + "</p>";
    }

    /** Le lien devient un bouton ; l'adresse reste lisible dessous, pour qui préfère la copier. */
    private static String bouton(String lien, String libelle) {
        return "<p style=\"margin:8px 0 12px;\"><a href=\"" + e(lien) + "\" style=\"display:inline-block;padding:12px 24px;border-radius:8px;background:" + CORAIL
                + ";color:#ffffff;font-size:16px;font-weight:700;text-decoration:none;" + POLICE + "\">" + e(libelle) + "</a></p>"
                + "<p style=\"margin:0 0 20px;font-size:12px;line-height:1.5;color:#6B7280;word-break:break-all;" + POLICE + "\">" + e(lien) + "</p>";
    }

    /** Tout texte — y compris celui qu'un visiteur a saisi — est échappé avant d'entrer dans le HTML. */
    private static String e(String texte) {
        return texte.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
