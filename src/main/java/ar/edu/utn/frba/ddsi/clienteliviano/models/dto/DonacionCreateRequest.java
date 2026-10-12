package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.List;

public record DonacionCreateRequest(
    String descripcion,
    Long donanteId,
    List<BienCreateRequest> bienes
) {
}
