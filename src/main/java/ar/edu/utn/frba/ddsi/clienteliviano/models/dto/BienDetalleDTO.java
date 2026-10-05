package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import lombok.Data;

@Data
public class BienDetalleDTO {
  private Long id;
  private String descripcion;
  private double cantidad;
  private boolean esUsado;
  private String fechaDeVencimiento;
  private double peso;
  private double volumen;
  private String foto;
}
