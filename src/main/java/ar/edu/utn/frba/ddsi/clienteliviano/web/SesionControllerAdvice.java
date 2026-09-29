package ar.edu.utn.frba.ddsi.clienteliviano.web;

import ar.edu.utn.frba.ddsi.clienteliviano.demo.DatosDemo;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expone el usuario y el contador de notificaciones a TODAS las vistas,
 * para que los navbars no obliguen a cada controller a cargarlos.
 */
@ControllerAdvice
public class SesionControllerAdvice {

  private final Sesion sesion;
  private final DatosDemo datos;

  public SesionControllerAdvice(Sesion sesion, DatosDemo datos) {
    this.sesion = sesion;
    this.datos = datos;
  }

  @ModelAttribute
  public void datosDeSesion(HttpSession session, Model model) {
    UsuarioActual usuario = sesion.usuario(session);
    model.addAttribute("usuarioActual", usuario);
    // ANDAMIO: el contador saldrá de la API cuando exista el GET de la bandeja
    // de notificaciones (brecha G8). Hoy lo provee DatosDemo.
    model.addAttribute("sinLeer", usuario == null ? 0L : datos.sinLeer(usuario.rol()));
  }
}
