package be.immoconnect.stockage;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Les photos sont servies comme de simples fichiers statiques sous /storage : jamais interprétées,
 * et accompagnées de l'en-tête « nosniff » posé par Spring Security (livrable 16).
 */
@Configuration
public class ConfigurationStockage implements WebMvcConfigurer {

    private final Path dossier;

    public ConfigurationStockage(@Value("${immoconnect.stockage.dossier}") Path dossier) {
        this.dossier = dossier;
    }

    @Bean
    StockageDisque stockagePhotos() {
        return new StockageDisque(dossier);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registre) {
        // Le nom d'une photo est aléatoire et son contenu ne change jamais : cache long
        String emplacement = dossier.toAbsolutePath().normalize().toUri().toString();
        registre.addResourceHandler(StockagePhotos.PREFIXE_URL + "**")
                .addResourceLocations(emplacement.endsWith("/") ? emplacement : emplacement + "/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }
}
