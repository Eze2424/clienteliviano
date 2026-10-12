package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Espejo de donaciones-service: GET /donaciones-service/entidades */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EntidadResponse(
    Long id,
    String razonSocial,
    String direccion,
    double latitud,
    double longitud,
    String telefono,
    List<NecesidadResponse> necesidades
) {
  public EntidadResponse(Long id, String razonSocial, String direccion, double latitud, double longitud, String telefono) {
    this(id, razonSocial, direccion, latitud, longitud, telefono, List.of());
  }

  @Override
  public List<NecesidadResponse> necesidades() {
    return necesidades != null ? necesidades : List.of();
  }
}
