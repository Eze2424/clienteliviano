package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import lombok.Data;

@Data
public class PerfilDonanteForm {
  private String nombre;
  private String apellido;
  private String razonSocial;
  private Boolean esJuridico = false;
  private String direccion;
  private String telefono;
  private String email;
}
