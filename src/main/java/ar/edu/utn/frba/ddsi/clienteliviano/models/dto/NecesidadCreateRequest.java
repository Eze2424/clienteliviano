package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

public record NecesidadCreateRequest(
    Long subcategoriaId,
    Long entidadId,
    String descripcion,
    double cantidadSolicitada,
    boolean esExtraordinaria
) {}
