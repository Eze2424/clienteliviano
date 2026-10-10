package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

/** Espejo de donaciones-service: necesidades de una entidad */
public record NecesidadResponse(
    Long id,
    String subcategoria,
    String descripcion,
    Double cantidadSolicitada,
    Boolean esExtraordinaria
) {
  public NecesidadResponse(Long id, String subcategoria, String descripcion) {
    this(id, subcategoria, descripcion, 0.0, false);
  }
}
