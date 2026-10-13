package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Una necesidad propuesta por los algoritmos, con la entidad que la pidió.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PropuestaVista(
    Long necesidadId,
    Long entidadId,
    String entidad,
    String entidadNombre,
    String subcategoria,
    String descripcion,
    Double cantidadSolicitada,
    Boolean esExtraordinaria,
    Double score
) {
  public PropuestaVista(
      Long necesidadId,
      Long entidadId,
      String entidad,
      String entidadNombre,
      String subcategoria,
      String descripcion,
      Double cantidadSolicitada,
      Boolean esExtraordinaria
  ) {
    this(necesidadId, entidadId, entidad, entidadNombre, subcategoria, descripcion, cantidadSolicitada, esExtraordinaria, null);
  }

  public PropuestaVista(
      Long necesidadId,
      Long entidadId,
      String entidad,
      String subcategoria,
      String descripcion,
      double cantidadSolicitada
  ) {
    this(necesidadId, entidadId, entidad, entidad, subcategoria, descripcion, cantidadSolicitada, false, null);
  }

  public String nombreEntidad() {
    return entidadNombre != null && !entidadNombre.isBlank()
        ? entidadNombre
        : (entidad != null && !entidad.isBlank() ? entidad : "Entidad #" + entidadId);
  }

  public String scoreFormateado() {
    return score != null ? String.format("%.2f", score) : null;
  }

  public String scorePorcentaje() {
    return score != null ? String.format("%.0f%%", score * 100) : null;
  }
}
