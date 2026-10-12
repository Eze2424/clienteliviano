package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfirmarAsignacionResponse(
    Long donacionId,
    Long entidadId,
    String entidadNombre,
    String estado,
    String advertencia
) {}
