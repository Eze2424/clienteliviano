package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DonacionIndependienteResumenResponse(
    Long id,
    String descripcion,
    String subcategoria,
    Long donanteId,
    String donanteNombre,
    String estado,
    LocalDate fechaVencimiento,
    boolean vencida,
    Long entidadAsignadaId,
    String entidadAsignadaNombre,
    boolean tienePropuestas
) {
}
