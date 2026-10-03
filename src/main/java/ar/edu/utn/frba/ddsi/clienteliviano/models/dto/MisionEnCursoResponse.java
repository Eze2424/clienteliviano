package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

/** Espejo de incentivos-service: GET /incentivos-service/{idDonante}/mision-en-curso */
public record MisionEnCursoResponse(
    String nombreMision,
    double progresoActual,
    double objetivo
) {
  /** Porcentaje para la barra de progreso. Presentación, no regla de negocio. */
  public int porcentaje() {
    if (objetivo <= 0) return 0;
    int p = (int) Math.round(progresoActual * 100 / objetivo);
    return Math.min(100, Math.max(0, p));
  }
}
