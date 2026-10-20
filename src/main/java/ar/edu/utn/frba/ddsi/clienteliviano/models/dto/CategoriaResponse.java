package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.List;

public record CategoriaResponse(
    Long id,
    String nombre,
    List<SubcategoriaResponse> subcategorias
) {}
