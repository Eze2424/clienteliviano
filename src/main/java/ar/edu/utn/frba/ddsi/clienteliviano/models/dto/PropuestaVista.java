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
    Boolean esExtraordinaria
) {
  public PropuestaVista(
      Long necesidadId,
      Long entidadId,
      String entidad,
      String subcategoria,
      String descripcion,
      double cantidadSolicitada
  ) {
    this(necesidadId, entidadId, entidad, entidad, subcategoria, descripcion, cantidadSolicitada, false);
  }

  public String nombreEntidad() {
    return entidadNombre != null && !entidadNombre.isBlank()
        ? entidadNombre
        : (entidad != null && !entidad.isBlank() ? entidad : "Entidad #" + entidadId);
  }
}
