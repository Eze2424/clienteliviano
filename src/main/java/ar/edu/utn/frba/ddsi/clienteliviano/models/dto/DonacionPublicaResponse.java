package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DonacionPublicaResponse(
    Long id,
    String descripcion,
    String subcategoria,
    String entidadNombre,
    String entidadDireccion,
    @JsonAlias({"entidadLatitud", "latitud"})
    Double latitud,
    @JsonAlias({"entidadLongitud", "longitud"})
    Double longitud,
    LocalDate fechaEntrega,
    @JsonAlias({"fotosComprobante", "fotos"})
    List<String> fotos,
    Integer cantidadBienes
) {
  @Override
  public String descripcion() {
    if (descripcion != null && !descripcion.isBlank()) {
      return descripcion;
    }
    if (subcategoria != null && !subcategoria.isBlank()) {
      return subcategoria;
    }
    return "Donación entregada";
  }

  @Override
  public Double latitud() {
    return latitud != null ? latitud : 0.0;
  }

  @Override
  public Double longitud() {
    return longitud != null ? longitud : 0.0;
  }

  @Override
  public Integer cantidadBienes() {
    return cantidadBienes != null ? cantidadBienes : 0;
  }

  @Override
  public List<String> fotos() {
    return fotos != null ? fotos : List.of();
  }
}
