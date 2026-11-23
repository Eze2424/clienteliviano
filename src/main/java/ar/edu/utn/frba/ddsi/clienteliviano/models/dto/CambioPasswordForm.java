package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import lombok.Data;

@Data
public class CambioPasswordForm {
  private String passwordActual;
  private String nuevaPassword;
  private String confirmacionPassword;
}
