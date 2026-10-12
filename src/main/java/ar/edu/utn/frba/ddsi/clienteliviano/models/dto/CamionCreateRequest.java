package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

public record CamionCreateRequest(
    String patente,
    double volumen,
    double altura,
    double capacidadDeCarga
) {}
