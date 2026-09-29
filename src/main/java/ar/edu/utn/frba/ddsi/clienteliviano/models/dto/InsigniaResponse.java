package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.time.LocalDateTime;

/** Espejo de incentivos-service: GET /incentivos-service/{idDonante}/insignias */
public record InsigniaResponse(
    String nombre,
    String urlImagen,
    LocalDateTime fechaObtencion,
    String descripcion
) {}
