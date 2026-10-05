package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class SubdonacionDTO {
  private Long id;
  private String subcategoria;
  private String categoria;
  private String estado;
  private String entidadNombre;
  private Long entidadId;
  private String fechaEntrega;
  private List<BienDetalleDTO> bienes = new ArrayList<>();
}
