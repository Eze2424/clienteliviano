package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

/** Espejo de donaciones-service: necesidades de una entidad */
public record NecesidadResponse(
    Long id,
    String subcategoria,
    String descripcion
) {}
