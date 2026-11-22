package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RutaResumenResponse(
    String idExterno,
    String estado,
    @JsonAlias({"camionPatente", "patenteCamion"})
    String patenteCamion,
    String camionId,
    List<ParadaResumenResponse> paradas
) {
  public List<ParadaResumenResponse> paradas() {
    return paradas != null ? paradas : List.of();
  }
}
