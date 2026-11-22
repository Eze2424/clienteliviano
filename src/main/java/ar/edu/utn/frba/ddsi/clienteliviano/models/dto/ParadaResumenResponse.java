package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParadaResumenResponse(
    int numeroParada,
    String entregaId,
    Long donacionId,
    String direccionDestino,
    @JsonAlias({"estado", "estadoEntrega"})
    String estadoEntrega,
    double latitud,
    double longitud
) {
}
