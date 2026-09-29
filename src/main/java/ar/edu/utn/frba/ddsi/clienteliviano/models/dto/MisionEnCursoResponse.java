package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

/** Espejo de incentivos-service: GET /incentivos-service/{idDonante}/mision-en-curso */
public record MisionEnCursoResponse(
    String nombreMision,
    double progresoActual,
    double objetivo
) {
  /** Porcentaje para la barra de progreso. Presentación, no regla de negocio. */
  public int porcentaje() {
    return objetivo <= 0 ? 0 : (int) Math.round(progresoActual * 100 / objetivo);
  }
}
