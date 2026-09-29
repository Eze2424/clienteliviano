package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Rutas /dashboard/* que quedaron de la primera version del equipo.
 * El area de cada rol vive ahora bajo su propio prefijo, para que la
 * proteccion por rol y el navbar sean por seccion. Se mantienen como
 * redirecciones para no romper enlaces ni marcadores.
 */
@Controller
public class DashboardController {

  @GetMapping("/dashboard/donante")
  public String donante() {
    return "redirect:/donante/dashboard";
  }

  @GetMapping("/dashboard/entidad")
  public String entidad() {
    return "redirect:/entidad/dashboard";
  }

  @GetMapping("/dashboard/staff")
  public String staff() {
    return "redirect:/staff/dashboard";
  }
}
