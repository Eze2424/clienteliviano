package ar.edu.utn.frba.ddsi.clienteliviano.models.vista;

import java.time.LocalDateTime;

/**
 * Una notificación recibida para mostrar en la vista.
 * El tipo se corresponde con el enum TiposDeNotificacion del backend.
 */
public record NotificacionVista(
    Long id,
    String tipo,
    String titulo,
    String detalle,
    LocalDateTime fecha,
    boolean leida
) {
  public NotificacionVista(String tipo, String titulo, String detalle, LocalDateTime fecha, boolean leida) {
    this(null, tipo, titulo, detalle, fecha, leida);
  }

  public String icono() {
    return switch (tipo != null ? tipo : "") {
      case "ASIGNACION_ENTIDAD", "ASIGNACION_DONANTE" -> "📦";
      case "ENTREGA_EXITOSA_ENTIDAD", "ENTREGA_EXITOSA_DONANTE" -> "✅";
      case "ENTREGA_FALLIDA" -> "⚠";
      case "MISION_COMPLETADA" -> "🏅";
      case "RUTA_INICIADA" -> "🚚";
      default -> "🔔";
    };
  }
}
