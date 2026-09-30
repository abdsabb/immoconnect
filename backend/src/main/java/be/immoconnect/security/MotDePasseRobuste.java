package be.immoconnect.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Règle de robustesse d'un mot de passe choisi par l'utilisateur (livrable 16, §2.1) : 8 à 72
 * caractères, une minuscule, une majuscule, un chiffre, et absent des fuites connues.
 * Elle s'applique au choix d'un mot de passe, jamais à la connexion.
 */
@Documented
@Constraint(validatedBy = ValidateurMotDePasse.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MotDePasseRobuste {

    String message() default "mot de passe trop faible";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
