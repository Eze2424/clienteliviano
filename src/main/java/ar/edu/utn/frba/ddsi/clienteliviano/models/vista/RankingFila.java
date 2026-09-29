package ar.edu.utn.frba.ddsi.clienteliviano.models.vista;

/**
 * Una fila del ranking mensual de donantes.
 *
 * BRECHA: incentivos-service define RankingMensualResponse(idDonante, nombre,
 * misionesCumplidas) y una interfaz RankingService, pero no hay implementación
 * ni endpoint que los exponga. Ver brecha G4. El orden y el puntaje los calcula
 * la API: el cliente solo renderiza la lista en el orden en que llega.
 */
public record RankingFila(
    int posicion,
    Long donanteId,
    String nombre,
    String tipoDonante,
    String categoria,
    long misionesCumplidas,
    int insignias,
    long puntajeImpacto
) {}
