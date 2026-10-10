package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

public record ResultadoOperacion(boolean exito, String mensaje) {
  public static ResultadoOperacion ok(String mensaje) {
    return new ResultadoOperacion(true, mensaje);
  }

  public static ResultadoOperacion error(String mensaje) {
    return new ResultadoOperacion(false, mensaje);
  }
}
