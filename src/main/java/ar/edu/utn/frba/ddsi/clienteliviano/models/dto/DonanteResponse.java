package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import ar.edu.utn.frba.ddsi.clienteliviano.models.entities.TipoOrganizacion;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Espejo de donaciones-service: GET /donaciones-service/donantes */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DonanteResponse(
    Long donanteId,
    String nombre,
    String apellido,
    Integer edad,
    String dni,
    String genero,
    String direccion,
    Boolean esJuridico,
    String razonSocial,
    String rubro,
    TipoOrganizacion tipoOrganizacion,
    String email,
    String telefono
) {
  public DonanteResponse(
      Long donanteId,
      String nombre,
      String apellido,
      Integer edad,
      String dni,
      String genero,
      String direccion,
      Boolean esJuridico,
      String razonSocial,
      String rubro,
      TipoOrganizacion tipoOrganizacion
  ) {
    this(donanteId, nombre, apellido, edad, dni, genero, direccion, esJuridico, razonSocial, rubro, tipoOrganizacion, null, null);
  }

  public Integer edad() {
    return edad != null ? edad : 0;
  }

  /** Cómo se lo nombra en pantalla: la razón social si es jurídico. */
  public String nombreVisible() {
    if (Boolean.TRUE.equals(esJuridico) && razonSocial != null && !razonSocial.isBlank()) {
      return razonSocial.trim();
    }
    String n = nombre != null ? nombre.trim() : "";
    String a = apellido != null ? apellido.trim() : "";
    if (n.isEmpty() && a.isEmpty()) {
      return "Donante";
    }
    if (a.isEmpty()) {
      return n;
    }
    if (n.isEmpty()) {
      return a;
    }
    return n + " " + a;
  }

  public String tipo() {
    return Boolean.TRUE.equals(esJuridico) ? "Organización" : "Persona";
  }
}
