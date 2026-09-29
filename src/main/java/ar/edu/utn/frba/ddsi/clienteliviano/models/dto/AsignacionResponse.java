package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.List;

/**
 * Espejo de donaciones-service: GET /donaciones-service/{id}/propuestas
 * Es el resultado de los algoritmos de selección. El cliente solo lo muestra:
 * la decisión de qué entidad gana la donación la toma la persona administradora.
 */
public record AsignacionResponse(
    Long donacionId,
    List<PropuestaVista> interseccion,
    List<PropuestaVista> subatendidos,
    List<PropuestaVista> semantico,
    Boolean hayCoincidencias
) {}
