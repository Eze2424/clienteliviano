package ar.edu.utn.frba.ddsi.clienteliviano.models.vista;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Una fila del ranking mensual de donantes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RankingFila(
    int posicion,
    Long donanteId,
    String nombre,
    String tipoDonante,
    String categoria,
    long misionesCumplidas,
    int insignias,
    long puntajeImpacto
) {
  public boolean esTop3() {
    return posicion >= 1 && posicion <= 3;
  }
}
