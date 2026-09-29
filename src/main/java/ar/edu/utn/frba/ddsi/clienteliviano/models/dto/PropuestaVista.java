package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

/**
 * Una necesidad propuesta por los algoritmos, con la entidad que la pidió.
 * El backend hoy devuelve NecesidadResponse sin la entidad; para elegir
 * destino la vista necesita saber de quién es. Ver brecha G6.
 */
public record PropuestaVista(
    Long necesidadId,
    Long entidadId,
    String entidad,
    String subcategoria,
    String descripcion,
    double cantidadSolicitada
) {}
