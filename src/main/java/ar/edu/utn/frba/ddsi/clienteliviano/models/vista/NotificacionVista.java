package ar.edu.utn.frba.ddsi.clienteliviano.models.vista;

import java.time.LocalDateTime;

/**
 * Una notificación recibida.
 *
 * BRECHA: notificaciones-service solo expone POST que ENVÍAN notificaciones
 * (mision-completada, asignacion-donante, asignacion-entidad, inactividad,
 * cambio-categoria). No hay ningún GET de la bandeja del usuario. Ver brecha G8.
 * El tipo se corresponde con el enum TiposDeNotificacion del backend.
 */
public record NotificacionVista(
    String tipo,
    String titulo,
    String detalle,
    LocalDateTime fecha,
    boolean leida
) {
  public String icono() {
    return switch (tipo) {
      case "ASIGNACION_ENTIDAD", "ASIGNACION_DONANTE" -> "📦";
      case "ENTREGA_EXITOSA_ENTIDAD", "ENTREGA_EXITOSA_DONANTE" -> "✅";
      case "ENTREGA_FALLIDA" -> "⚠";
      case "MISION_COMPLETADA" -> "🏅";
      case "RUTA_INICIADA" -> "🚚";
      default -> "🔔";
    };
  }
}
