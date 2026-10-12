package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

public record CamionUpdateRequest(
    String patente,
    double volumen,
    double altura,
    double capacidadDeCarga
) {}
