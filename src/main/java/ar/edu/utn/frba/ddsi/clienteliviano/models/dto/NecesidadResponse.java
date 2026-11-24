package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.time.LocalDate;

/** Espejo de donaciones-service: necesidades de una entidad */
public record NecesidadResponse(
    Long id,
    String subcategoria,
    String descripcion,
    Double cantidadSolicitada,
    Boolean esExtraordinaria,
    Double cantidadCubierta,
    Boolean satisfecha,
    String periodicidad,
    LocalDate periodoDesde,
    LocalDate periodoHasta,
    LocalDate renuevaEl
) {
  public NecesidadResponse(Long id, String subcategoria, String descripcion) {
    this(id, subcategoria, descripcion, 0.0, false);
  }

  public NecesidadResponse(Long id, String subcategoria, String descripcion,
                           Double cantidadSolicitada, Boolean esExtraordinaria) {
    this(id, subcategoria, descripcion, cantidadSolicitada, esExtraordinaria, 0.0, false);
  }
  public NecesidadResponse(Long id, String subcategoria, String descripcion, Double cantidadSolicitada,
                           Boolean esExtraordinaria, Double cantidadCubierta, Boolean satisfecha) {
    this(id, subcategoria, descripcion, cantidadSolicitada, esExtraordinaria, cantidadCubierta, satisfecha,
        null, null, null, null);
  }
  public String periodicidadTexto() {
    if (Boolean.TRUE.equals(esExtraordinaria)) {
      return null;
    }
    if (periodicidad == null) {
      return "Mensual";
    }
    return switch (periodicidad) {
      case "SEMANAL" -> "Semanal";
      case "QUINCENAL" -> "Quincenal";
      default -> "Mensual";
    };
  }
  public double cantidadRestante() {
    double solicitada = cantidadSolicitada != null ? cantidadSolicitada : 0;
    double cubierta = cantidadCubierta != null ? cantidadCubierta : 0;
    return Math.max(0, solicitada - cubierta);
  }

  public int porcentajeCubierto() {
    if (cantidadSolicitada == null || cantidadSolicitada <= 0 || cantidadCubierta == null) {
      return 0;
    }
    return (int) Math.min(100, Math.round(cantidadCubierta / cantidadSolicitada * 100));
  }
}
