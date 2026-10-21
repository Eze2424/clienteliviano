package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

/** Espejo de donaciones-service: necesidades de una entidad */
public record NecesidadResponse(
    Long id,
    String subcategoria,
    String descripcion,
    Double cantidadSolicitada,
    Boolean esExtraordinaria,
    Double cantidadCubierta,
    Boolean satisfecha
) {
  public NecesidadResponse(Long id, String subcategoria, String descripcion) {
    this(id, subcategoria, descripcion, 0.0, false);
  }

  public NecesidadResponse(Long id, String subcategoria, String descripcion,
                           Double cantidadSolicitada, Boolean esExtraordinaria) {
    this(id, subcategoria, descripcion, cantidadSolicitada, esExtraordinaria, 0.0, false);
  }

  public int porcentajeCubierto() {
    if (cantidadSolicitada == null || cantidadSolicitada <= 0 || cantidadCubierta == null) {
      return 0;
    }
    return (int) Math.min(100, Math.round(cantidadCubierta / cantidadSolicitada * 100));
  }
}
