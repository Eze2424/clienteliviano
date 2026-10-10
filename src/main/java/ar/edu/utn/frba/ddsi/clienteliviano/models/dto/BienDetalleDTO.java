package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BienDetalleDTO {
  private Long id;
  private String descripcion;
  private double cantidad;
  private boolean esUsado;
  private String fechaDeVencimiento;
  private double peso;

  private double volumen;
  private String foto;

  public BienDetalleDTO(String descripcion, int cantidad) {
    this.descripcion = descripcion;
    this.cantidad = cantidad;
  }
}
