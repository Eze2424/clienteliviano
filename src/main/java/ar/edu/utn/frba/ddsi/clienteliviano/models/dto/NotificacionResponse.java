package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.time.LocalDateTime;

public record NotificacionResponse(
    Long id,
    String tipo,
    String titulo,
    String detalle,
    LocalDateTime fecha,
    boolean leida
) {
}
