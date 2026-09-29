package ar.edu.utn.frba.ddsi.clienteliviano.models.vista;

import java.time.LocalDateTime;

/**
 * Una entrega en curso, con el camión que la lleva.
 *
 * BRECHA: logistica-service expone GET /camiones (patente y posición) y
 * EntregaEstadoResponse(idEntrega, donacionId, estadoActual), pero nada
 * relaciona una entrega con la entidad que la espera ni informa la hora de la
 * última posición. Ver brecha G3. La posición y la hora vienen de la API:
 * el cliente no las estima.
 */
import ar.edu.utn.frba.ddsi.clienteliviano.web.ProyeccionMapa;

public record EntregaVista(
    String idEntrega,
    Long donacionId,
    String descripcionDonacion,
    String patenteCamion,
    String conductor,
    String estado,
    double latitud,
    double longitud,
    LocalDateTime ultimaActualizacion,
    String llegadaEstimada
) {
  public String etiquetaEstado() {
    return switch (estado) {
      case "LISTA" -> "Lista para despacho";
      case "EN_TRASLADO" -> "En traslado";
      case "ENTREGADA" -> "Entregada";
      case "ENTREGA_FALLIDA" -> "Entrega fallida";
      default -> estado;
    };
  }

  public String claseBadge() {
    return switch (estado) {
      case "ENTREGADA" -> "badge-success";
      case "EN_TRASLADO" -> "badge-info";
      case "ENTREGA_FALLIDA" -> "badge-danger";
      default -> "badge-neutral";
    };
  }

  /** Posición del pin en el mapa esquemático. Ver ProyeccionMapa. */
  public int posX() {
    return ProyeccionMapa.x(longitud);
  }

  public int posY() {
    return ProyeccionMapa.y(latitud);
  }
}
