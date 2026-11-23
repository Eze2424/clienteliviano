package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

public record DonantePerfilUpdateRequest(
    String nombre,
    String apellido,
    String razonSocial,
    String direccion,
    String telefono
) {}
