package ar.edu.utn.frba.ddsi.clienteliviano.models.vista;

import java.time.LocalDate;
import java.util.List;

/**
 * Forma que las vistas necesitan para una donación.
 * Permite representar tanto el estado consolidado como el detalle
 * de cada donación independiente asociada a sus bienes.
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
    int cantidadBienes,
    String imagenUrl,
    List<SubdonacionVista> subdonaciones
) {

  public DonacionVista(
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
      int cantidadBienes,
      String imagenUrl
  ) {
    this(id, descripcion, estado, fecha, vencimiento, vencida, donanteId, donante, entidadId, entidad, cantidadBienes, imagenUrl, List.of());
  }

  public boolean tieneSubdonaciones() {
    return subdonaciones != null && !subdonaciones.isEmpty();
  }

  /**
   * Estado consolidado para la card:
   * Sintetiza las diferentes etapas de las donaciones independientes pertenecientes a esta donación.
   */
  public String etiquetaConsolidada() {
    if (subdonaciones == null || subdonaciones.isEmpty()) {
      return etiquetaEstado();
    }
    if (subdonaciones.size() == 1) {
      return subdonaciones.get(0).etiquetaEstado();
    }

    long total = subdonaciones.size();
    long entregadas = subdonaciones.stream().filter(s -> "ENTREGADA".equalsIgnoreCase(s.estado())).count();
    long enTraslado = subdonaciones.stream().filter(s -> "EN_TRASLADO".equalsIgnoreCase(s.estado())).count();
    long asignadas = subdonaciones.stream().filter(s -> "ASIGNADA".equalsIgnoreCase(s.estado())).count();
    long listas = subdonaciones.stream().filter(s -> "LISTA".equalsIgnoreCase(s.estado())).count();
    long enDeposito = subdonaciones.stream().filter(s -> "EN_DEPOSITO".equalsIgnoreCase(s.estado())).count();

    if (entregadas == total) {
      return "Entregada (" + total + "/" + total + ")";
    }
    if (entregadas > 0) {
      return entregadas + " de " + total + " entregadas";
    }
    if (enTraslado > 0) {
      return enTraslado + " de " + total + " en traslado";
    }
    if (listas > 0) {
      return listas + " listas para despacho";
    }
    if (asignadas == total) {
      return "Asignada";
    }
    if (asignadas > 0) {
      return asignadas + " de " + total + " asignadas";
    }
    if (enDeposito == total) {
      return "En depósito";
    }
    return etiquetaEstado();
  }

  public String claseBadgeConsolidada() {
    if (subdonaciones == null || subdonaciones.isEmpty() || subdonaciones.size() == 1) {
      return claseBadge();
    }
    long total = subdonaciones.size();
    long entregadas = subdonaciones.stream().filter(s -> "ENTREGADA".equalsIgnoreCase(s.estado())).count();
    long enTraslado = subdonaciones.stream().filter(s -> "EN_TRASLADO".equalsIgnoreCase(s.estado())).count();
    long asignadas = subdonaciones.stream().filter(s -> "ASIGNADA".equalsIgnoreCase(s.estado())).count();

    if (entregadas == total) {
      return "badge-success";
    }
    if (entregadas > 0 || enTraslado > 0) {
      return "badge-info";
    }
    if (asignadas > 0) {
      return "badge-warning";
    }
    return "badge-neutral";
  }

  /** Traducción del enum TipoEstado del backend a texto para la persona. */
  public String etiquetaEstado() {
    if (estado == null) return "En depósito";
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
    if (estado == null) return "badge-neutral";
    return switch (estado) {
      case "ENTREGADA" -> "badge-success";
      case "EN_TRASLADO", "LISTA" -> "badge-info";
      case "ASIGNADA" -> "badge-warning";
      case "ENTREGA_FALLIDA" -> "badge-danger";
      default -> "badge-neutral";
    };
  }

  public boolean asignada() {
    return entidadId != null || (entidad != null && !entidad.isBlank() && !entidad.equalsIgnoreCase("Sin asignar") && !entidad.equalsIgnoreCase("Todavía sin asignar"));
  }
}
