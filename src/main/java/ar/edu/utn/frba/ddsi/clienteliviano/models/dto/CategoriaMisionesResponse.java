package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.List;

public record CategoriaMisionesResponse(
    Long id,
    String nombreCategoria,
    String siguienteCategoria,
    List<MisionResponse> misiones
) {
  public List<MisionResponse> misiones() {
    return misiones != null ? misiones : List.of();
  }
}
