package be.immoconnect.dto;

import be.immoconnect.entities.Categorie;

/** Catégorie de biens, telle que l'API la renvoie. */
public record CategorieResume(Integer id, String nom, String description) {

    public static CategorieResume depuis(Categorie categorie) {
        return new CategorieResume(categorie.getId(), categorie.getNom(), categorie.getDescription());
    }
}
