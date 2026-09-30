package be.immoconnect.controllers;

import jakarta.servlet.http.HttpServletRequest;

/** Lecture des informations de la requête HTTP communes à tous les contrôleurs. */
public final class RequeteHttp {

    private RequeteHttp() {
    }

    /** Adresse IP réelle, en tenant compte du proxy Nginx (X-Forwarded-For) — pour le journal d'audit. */
    public static String adresseIp(HttpServletRequest http) {
        String transmise = http.getHeader("X-Forwarded-For");
        return transmise != null && !transmise.isBlank() ? transmise.split(",")[0].trim() : http.getRemoteAddr();
    }
}
