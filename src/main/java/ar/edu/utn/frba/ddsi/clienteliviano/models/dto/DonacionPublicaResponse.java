package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.time.LocalDate;
import java.util.List;

public record DonacionPublicaResponse(
    Long id,
    String descripcion,
    String subcategoria,
    String entidadNombre,
    String entidadDireccion,
    double latitud,
    double longitud,
    LocalDate fechaEntrega,
    List<String> fotos,
    int cantidadBienes
) {
  public List<String> fotos() {
    return fotos != null ? fotos : List.of();
  }
}
