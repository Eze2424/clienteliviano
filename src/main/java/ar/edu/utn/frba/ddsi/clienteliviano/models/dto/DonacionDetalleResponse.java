package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DonacionDetalleResponse(
    Long id,
    String descripcion,
    String estado,
    LocalDate fecha,
    LocalDate vencimiento,
    String donanteNombre,
    Long donanteId,
    Long entidadId,
    String entidadNombre,
    int cantidadBienes,
    List<BienDetalleDTO> bienes
) {}
