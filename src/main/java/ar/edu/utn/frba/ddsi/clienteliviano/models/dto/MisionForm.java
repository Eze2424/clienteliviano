package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MisionForm {
  private String tipo;
  private String nombre;
  private Double objetivo;
}
