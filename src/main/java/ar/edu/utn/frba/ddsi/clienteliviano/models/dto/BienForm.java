package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.time.LocalDate;
import lombok.Data;

/** Un bien dentro de una donación. Espeja BienCreateRequest de donaciones-service. */
@Data
public class BienForm {

  private String descripcion;
  private Long categoriaId;
  private Long subcategoriaId;
  private String foto;
  private Double cantidad;
  private boolean esUsado;
  private LocalDate fechaDeVencimiento;
  private Double peso;
  private Double volumen;
}
