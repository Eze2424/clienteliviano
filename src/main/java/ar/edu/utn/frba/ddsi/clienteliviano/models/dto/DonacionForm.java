package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Alta de una donación recibida en el depósito, a nombre de un donante.
 * Espeja DonacionCreateRequest(descripcion, donanteId, bienes).
 */
@Data
public class DonacionForm {

  private String descripcion;
  private Long donanteId;
  private List<BienForm> bienes = new ArrayList<>(List.of(new BienForm()));
}
