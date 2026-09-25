package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import lombok.Data;

@Data
public class DonacionResumenDTO {
  private Long id;
  private String titulo;
  private String entidadNombre;
  private String fecha;
  private String estado;
  private String descripcionBreve;
  private String imagenUrl;
}
