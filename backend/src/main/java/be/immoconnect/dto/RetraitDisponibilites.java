package be.immoconnect.dto;

/** Bilan du retrait de disponibilités par un agent : plages retirées et visites annulées par la même occasion. */
public record RetraitDisponibilites(int plagesRetirees, int visitesAnnulees) {
}
