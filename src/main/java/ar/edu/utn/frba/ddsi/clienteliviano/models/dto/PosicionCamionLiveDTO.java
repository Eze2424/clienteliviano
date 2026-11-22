package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

public record PosicionCamionLiveDTO(
    String id,
    String patente,
    double latitud,
    double longitud,
    String ultimaActualizacionTexto
) {
}
