package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

public record MisionResponse(
    Long id,
    String tipo,
    String nombre,
    double objetivo,
    String descripcionInsignia
) {
  public String etiquetaTipo() {
    if (tipo == null) {
      return "Sin especificar";
    }
    return switch (tipo) {
      case "HABIL_DONADOR" -> "Cantidad de bienes donados";
      case "DONACIONES_EXITOSAS" -> "Donaciones entregadas";
      case "COMPLETITUD" -> "Categorías distintas";
      case "RACHA" -> "Meses consecutivos (racha)";
      default -> tipo;
    };
  }

  public String unidadObjetivo() {
    if (tipo == null) {
      return "unidades";
    }
    return switch (tipo) {
      case "HABIL_DONADOR" -> "bienes";
      case "DONACIONES_EXITOSAS" -> "entregas";
      case "COMPLETITUD" -> "categorías";
      case "RACHA" -> "meses";
      default -> "unidades";
    };
  }

  public String objetivoFormateado() {
    if (objetivo == (long) objetivo) {
      return String.valueOf((long) objetivo);
    }
    return String.valueOf(objetivo);
  }
}
