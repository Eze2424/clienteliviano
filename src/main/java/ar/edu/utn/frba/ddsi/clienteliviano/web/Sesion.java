package ar.edu.utn.frba.ddsi.clienteliviano.web;

import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * Acceso a la identidad guardada en la sesión del servidor.
 *
 * El token de la API y el usuario viven acá, nunca en localStorage ni en una
 * cookie legible por JavaScript. RestClientConfig lee el token de la misma
 * sesión para firmar cada llamada saliente.
 */
@Component
public class Sesion {

  public static final String USUARIO = "usuarioActual";
  public static final String TOKEN = "JWT_TOKEN";

  public UsuarioActual usuario(HttpSession session) {
    return (UsuarioActual) session.getAttribute(USUARIO);
  }

  public void iniciar(HttpSession session, UsuarioActual usuario) {
    session.setAttribute(USUARIO, usuario);
  }

  /** ¿Hay sesión y el rol es el que la sección exige? */
  public boolean tieneRol(HttpSession session, Rol rol) {
    UsuarioActual u = usuario(session);
    return u != null && u.rol() == rol;
  }
}
