package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

/** Espejo de logistica-service: GET /camiones */
import ar.edu.utn.frba.ddsi.clienteliviano.web.ProyeccionMapa;

public record CamionResponse(
    String id,
    String patente,
    double volumen,
    double altura,
    double capacidadDeCarga,
    double latitud,
    double longitud
) {
  /** Posición del pin en el mapa esquemático. Ver ProyeccionMapa. */
  public int posX() {
    return ProyeccionMapa.x(longitud);
  }

  public int posY() {
    return ProyeccionMapa.y(latitud);
  }
}
