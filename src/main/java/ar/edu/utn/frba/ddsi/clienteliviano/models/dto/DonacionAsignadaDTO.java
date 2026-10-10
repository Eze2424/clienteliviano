package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.time.LocalDate;
import java.util.List;

public record DonacionAsignadaDTO(
    Long id,
    String descripcion,
    String estado,
    LocalDate fecha,
    LocalDate vencimiento,
    String donanteNombre,
    Long donanteId,
    int cantidadBienes,
    List<BienDetalleDTO> bienes
) {}
