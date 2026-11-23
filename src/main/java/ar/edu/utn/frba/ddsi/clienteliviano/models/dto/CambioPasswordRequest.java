package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

public record CambioPasswordRequest(
    String passwordActual,
    String nuevaPassword
) {}
