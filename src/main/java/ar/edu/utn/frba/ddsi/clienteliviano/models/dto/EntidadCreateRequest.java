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
  private String password;
  private String medioPredeterminado;
}
