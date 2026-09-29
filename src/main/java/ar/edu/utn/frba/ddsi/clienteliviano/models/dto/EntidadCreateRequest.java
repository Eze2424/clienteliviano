package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import lombok.Data;

@Data
public class EntidadCreateRequest {
  private String razonSocial;
  private String direccion;
  private String telefono;
  private double latitud;
  private double longitud;
  private String email;

  /**
   * Solo para dar de alta la identidad en Keycloak. El DonanteCreateRequest del
   * backend NO tiene este campo, asi que no debe viajar en el cuerpo que se le
   * manda: al armar el request de la API hay que excluirlo.
   */
  private String password;
  private String medioPredeterminado;
}
