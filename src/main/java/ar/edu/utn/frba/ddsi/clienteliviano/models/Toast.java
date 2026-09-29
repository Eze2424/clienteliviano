package ar.edu.utn.frba.ddsi.clienteliviano.models;

/**
 * Notificación no intrusiva que el controller deja como flash attribute
 * antes de redirigir (patrón Post/Redirect/Get). La renderiza
 * fragments/componentes :: flash-toast y la muestra main.js al cargar.
 */
public record Toast(String tipo, String mensaje) {

  public static Toast exito(String mensaje) {
    return new Toast("success", mensaje);
  }

  public static Toast error(String mensaje) {
    return new Toast("error", mensaje);
  }

  public String getTipo() {
    return tipo;
  }

  public String getMensaje() {
    return mensaje;
  }
}
