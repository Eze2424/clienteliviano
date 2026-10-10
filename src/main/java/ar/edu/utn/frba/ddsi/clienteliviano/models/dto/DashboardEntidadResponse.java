package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.List;

public record DashboardEntidadResponse(
    String razonSocial,
    int totalAsignadas,
    int entregadas,
    int entregasActivas,
    int necesidadesActivas,
    List<DonacionAsignadaDTO> donaciones
) {}
