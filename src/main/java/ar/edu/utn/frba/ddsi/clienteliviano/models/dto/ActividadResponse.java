package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.Map;

/** Espejo de incentivos-service: GET /incentivos-service/{idDonante}/actividad */
public record ActividadResponse(
    int totalHistoricoDonaciones,
    int recordDonaciones,
    int misionesCompletadas,
    int organizacionesAyudadas,
    String periodoConsultado,
    Integer donacionesEnPeriodo,
    Long misionesEnPeriodo,
    Map<String, Integer> evolucionMensual
) {}
