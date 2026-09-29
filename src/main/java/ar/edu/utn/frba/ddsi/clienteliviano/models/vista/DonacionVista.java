package ar.edu.utn.frba.ddsi.clienteliviano.models.vista;

import java.time.LocalDate;

/**
 * Forma que las vistas necesitan para una donación.
 *
 * BRECHA: donaciones-service hoy devuelve DonacionResponse(donacionId, descripcion,
 * donanteId) y nada más. El estado, la fecha, el vencimiento y la entidad destino
 * no están expuestos (EstadosController está comentado en el backend).
 * Ver brechas G1 y G3. Cuando la API los provea, este record se llena desde ella;
 * el cliente NO los calcula.
 */
public record DonacionVista(
    Long id,
    String descripcion,
    String estado,
    LocalDate fecha,
    LocalDate vencimiento,
    boolean vencida,
    Long donanteId,
    String donante,
    Long entidadId,
    String entidad,
    int cantidadBienes
) {
  /** Traducción del enum TipoEstado del backend a texto para la persona. */
  public String etiquetaEstado() {
    return switch (estado) {
      case "EN_DEPOSITO" -> "En depósito";
      case "ASIGNADA" -> "Asignada";
      case "LISTA" -> "Lista para despacho";
      case "EN_TRASLADO" -> "En traslado";
      case "ENTREGADA" -> "Entregada";
      case "ENTREGA_FALLIDA" -> "Entrega fallida";
      default -> estado;
    };
  }

  /** Clase del badge. El badge siempre lleva texto: nunca comunica solo por color. */
  public String claseBadge() {
    return switch (estado) {
      case "ENTREGADA" -> "badge-success";
      case "EN_TRASLADO", "LISTA" -> "badge-info";
      case "ASIGNADA" -> "badge-warning";
      case "ENTREGA_FALLIDA" -> "badge-danger";
      default -> "badge-neutral";
    };
  }

  public boolean asignada() {
    return entidadId != null;
  }
}
