package ar.edu.utn.frba.ddsi.clienteliviano.models;

/**
 * Identidad del usuario autenticado, tal como la muestran los navbars.
 * Vive en la sesión del servidor junto al token; nunca se expone al navegador
 * más que como texto renderizado.
 */
public record UsuarioActual(Long id, String nombre, String email, Rol rol) {

  public String getIniciales() {
    String[] partes = nombre.trim().split("\\s+");
    if (partes.length == 1) {
      return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
    }
    return ("" + partes[0].charAt(0) + partes[partes.length - 1].charAt(0)).toUpperCase();
  }

  public String getEtiquetaRol() {
    return rol.getEtiqueta();
  }

  public String getNombre() {
    return nombre;
  }
}
