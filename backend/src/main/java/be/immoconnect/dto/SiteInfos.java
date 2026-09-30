package be.immoconnect.dto;

import java.util.List;

/** Identité de l'agence et langues proposées, réglées par l'administrateur (cas A5) et lues par tout le site. */
public record SiteInfos(String nom, String slogan, String adresse, String telephone, String email, String horaires,
                        List<String> languesActives) {
}
