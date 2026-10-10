package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.time.LocalDate;
import java.util.List;

public record DonacionDetalleDTO(
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
