package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

/** Espejo de donaciones-service: GET /donaciones-service/entidades */
public record EntidadResponse(
    Long id,
    String razonSocial,
    String direccion,
    double latitud,
    double longitud,
    String telefono
) {}
