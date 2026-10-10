package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.time.LocalDateTime;

public record EntregaSeguimientoDTO(
    String idEntrega,
    Long donacionId,
    String estadoActual,
    String patenteCamion,
    Double latitud,
    Double longitud,
    LocalDateTime ultimaActualizacion
) {
}
