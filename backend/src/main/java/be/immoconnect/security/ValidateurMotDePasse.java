package be.immoconnect.security;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Applique la règle {@link MotDePasseRobuste} et nomme la première exigence non remplie. */
public class ValidateurMotDePasse implements ConstraintValidator<MotDePasseRobuste, String> {

    static final int LONGUEUR_MIN = 8;
    /** BCrypt ignore ce qui dépasse 72 octets : un mot de passe plus long donnerait une fausse sécurité. */
    static final int LONGUEUR_MAX = 72;

    private final MotsDePasseCompromis compromis;

    public ValidateurMotDePasse(MotsDePasseCompromis compromis) {
        this.compromis = compromis;
    }

    @Override
    public boolean isValid(String motDePasse, ConstraintValidatorContext contexte) {
        if (motDePasse == null || motDePasse.isBlank()) {
            return true; // l'obligation de saisie est portée par @NotBlank
        }
        String defaut = defaut(motDePasse);
        if (defaut == null) {
            return true;
        }
        contexte.disableDefaultConstraintViolation();
        contexte.buildConstraintViolationWithTemplate(defaut).addConstraintViolation();
        return false;
    }

    /** @return l'exigence non remplie, ou {@code null} si le mot de passe convient */
    String defaut(String motDePasse) {
        if (motDePasse.length() < LONGUEUR_MIN) {
            return "le mot de passe doit compter au moins 8 caractères";
        }
        if (motDePasse.length() > LONGUEUR_MAX) {
            return "le mot de passe ne peut dépasser 72 caractères";
        }
        if (motDePasse.chars().noneMatch(Character::isLowerCase) || motDePasse.chars().noneMatch(Character::isUpperCase)) {
            return "le mot de passe doit mêler minuscules et majuscules";
        }
        if (motDePasse.chars().noneMatch(Character::isDigit)) {
            return "le mot de passe doit contenir au moins un chiffre";
        }
        if (compromis.contient(motDePasse)) {
            return "ce mot de passe figure dans une fuite de données connue, choisissez-en un autre";
        }
        return null;
    }
}
