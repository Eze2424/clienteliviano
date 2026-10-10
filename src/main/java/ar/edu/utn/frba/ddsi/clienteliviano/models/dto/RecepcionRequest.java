package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.List;

public record RecepcionRequest(
    List<String> fotoUrls,
    String observaciones,
    Boolean completa
) {}
