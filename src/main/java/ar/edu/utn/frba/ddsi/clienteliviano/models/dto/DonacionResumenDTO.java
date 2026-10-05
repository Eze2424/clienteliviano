package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Una donacion dentro del resumen del dashboard del donante.
 * Contiene el listado de subdonaciones (donaciones independientes categorizadas).
 */
@Data
public class DonacionResumenDTO {
  private Long id;
  private String titulo;
  private String entidadNombre;
  private String fecha;
  private String estado;
  private String descripcionBreve;
  private String imagenUrl;
  private List<SubdonacionDTO> subdonaciones = new ArrayList<>();
}
