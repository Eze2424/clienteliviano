package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

/** Espejo de logistica-service: GET /camiones */
import ar.edu.utn.frba.ddsi.clienteliviano.web.ProyeccionMapa;
import java.time.LocalDateTime;

public record CamionResponse(
    String id,
    String patente,
    double volumen,
    double altura,
    double capacidadDeCarga,
    double latitud,
    double longitud,
    LocalDateTime ultimaActualizacion
) {
  public CamionResponse(String id, String patente, double volumen, double altura, double capacidadDeCarga, double latitud, double longitud) {
    this(id, patente, volumen, altura, capacidadDeCarga, latitud, longitud, null);
  }

  /** Posición del pin en el mapa esquemático. Ver ProyeccionMapa. */
  public int posX() {
    return ProyeccionMapa.x(longitud);
  }

  public int posY() {
    return ProyeccionMapa.y(latitud);
  }
}

