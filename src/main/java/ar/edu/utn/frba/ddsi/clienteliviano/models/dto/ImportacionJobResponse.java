package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.List;

public record ImportacionJobResponse(
    String jobId,
    String estado,
    int procesadas,
    int total,
    int creados,
    int actualizados,
    List<FilaErrorDto> errores,
    String mensaje
) {
  public int porcentaje() {
    if (total <= 0) {
      return "FINALIZADO".equalsIgnoreCase(estado) ? 100 : 0;
    }
    return Math.min(100, (int) Math.round(((double) procesadas / total) * 100));
  }
}
