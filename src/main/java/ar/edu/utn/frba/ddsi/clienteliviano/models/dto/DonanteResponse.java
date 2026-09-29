package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import ar.edu.utn.frba.ddsi.clienteliviano.models.entities.TipoOrganizacion;

/** Espejo de donaciones-service: GET /donaciones-service/donantes */
public record DonanteResponse(
    Long donanteId,
    String nombre,
    String apellido,
    int edad,
    String dni,
    String genero,
    String direccion,
    Boolean esJuridico,
    String razonSocial,
    String rubro,
    TipoOrganizacion tipoOrganizacion
) {
  /** Cómo se lo nombra en pantalla: la razón social si es jurídico. */
  public String nombreVisible() {
    return Boolean.TRUE.equals(esJuridico) && razonSocial != null
        ? razonSocial
        : nombre + " " + apellido;
  }

  public String tipo() {
    return Boolean.TRUE.equals(esJuridico) ? "Organización" : "Persona";
  }
}
