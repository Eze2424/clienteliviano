package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.time.LocalDate;

public record BienCreateRequest(
    String descripcion,
    String foto,
    Long subcategoriaId,
    double cantidad,
    boolean esUsado,
    LocalDate fechaDeVencimiento,
    double peso,
    double volumen
) {
}
