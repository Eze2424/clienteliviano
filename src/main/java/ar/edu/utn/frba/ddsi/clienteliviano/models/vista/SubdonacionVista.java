package ar.edu.utn.frba.ddsi.clienteliviano.models.vista;

import java.util.List;

public record SubdonacionVista(
    Long id,
    String subcategoria,
    String categoria,
    String estado,
    String entidad,
    Long entidadId,
    String fechaEntrega,
    int cantidadBienes,
    String bienesResumen,
    List<BienDetalleVista> bienes
) {
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
