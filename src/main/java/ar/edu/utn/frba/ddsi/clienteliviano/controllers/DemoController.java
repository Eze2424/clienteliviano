package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.demo.DatosDemo;
import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.web.Sesion;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ANDAMIO DE DESARROLLO — no forma parte del entregable final.
 *
 * Permite recorrer las vistas de cada rol sin Keycloak levantado.
 * Cuando el login real contra Keycloak funcione, se borra este archivo entero:
 * ningún otro controller depende de él.
 *
 *   GET /demo              elegir rol
 *   GET /demo/donante      entrar como donante
 *   GET /demo/entidad      entrar como entidad beneficiaria
 *   GET /demo/admin        entrar como administración
 */
@Controller
@RequestMapping("/demo")
public class DemoController {

  private final Sesion sesion;
  private final DatosDemo datos;

  public DemoController(Sesion sesion, DatosDemo datos) {
    this.sesion = sesion;
    this.datos = datos;
  }

  @GetMapping
  public String elegirRol(Model model) {
    model.addAttribute("roles", Rol.values());
    return "demo/roles";
  }

  @GetMapping("/{rol}")
  public String entrarComo(@PathVariable String rol, HttpSession session) {
    Rol elegido = switch (rol.toLowerCase()) {
      case "donante" -> Rol.DONANTE;
      case "entidad" -> Rol.ENTIDAD;
      case "admin", "staff" -> Rol.ADMIN;
      default -> null;
    };
    if (elegido == null) {
      return "redirect:/demo";
    }
    sesion.iniciar(session, datos.usuario(elegido));
    return "redirect:" + elegido.getInicio();
  }
}
