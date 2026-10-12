package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

public record ResultadoImportacion(
    boolean exito,
    String mensaje,
    String jobId
) {
  public static ResultadoImportacion ok(String mensaje, String jobId) {
    return new ResultadoImportacion(true, mensaje, jobId);
  }

  public static ResultadoImportacion error(String mensaje) {
    return new ResultadoImportacion(false, mensaje, null);
  }
}
