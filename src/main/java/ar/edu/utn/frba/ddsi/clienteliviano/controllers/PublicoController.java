package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.Toast;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.RegistroForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.entities.TipoOrganizacion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Vistas abiertas: portada, alta de cuenta y textos legales.
 * El controller no habla HTTP con la API: cuando haya servicio cliente,
 * se inyecta acá y estos métodos siguen igual de delgados.
 */
@Controller
public class PublicoController {

  @GetMapping("/")
  public String portada() {
    return "publico/index";
  }

  @GetMapping("/legal")
  public String legal() {
    return "publico/legal";
  }

  @GetMapping("/registro")
  public String formularioRegistro(Model model) {
    model.addAttribute("form", new RegistroForm());
    model.addAttribute("tiposOrganizacion", TipoOrganizacion.values());
    return "publico/registro";
  }

  @PostMapping("/registro")
  public String registrar(@ModelAttribute("form") RegistroForm form,
                          Model model,
                          RedirectAttributes redirect) {

    // Validación de formato para dar feedback rápido. La validación de dominio
    // (email ya usado, documento inválido) la resuelve la API, no el cliente.
    if (form.getPassword() == null || !form.getPassword().equals(form.getPasswordConfirmacion())) {
      model.addAttribute("tiposOrganizacion", TipoOrganizacion.values());
      model.addAttribute("error", "Las dos contraseñas no coinciden. Revisalas y probá de nuevo.");
      return "publico/registro";
    }
    if (!form.isAceptaTerminos()) {
      model.addAttribute("tiposOrganizacion", TipoOrganizacion.values());
      model.addAttribute("error", "Para crear la cuenta necesitás aceptar los términos.");
      return "publico/registro";
    }

    // PENDIENTE (equipo): acá va la llamada real.
    //   form.esEntidad() -> POST {donaciones}/entidades  con EntidadCreateRequest
    //   si no            -> POST {donaciones}/donantes   con DonanteCreateRequest
    // más el alta de identidad en Keycloak, que hoy resuelve RegisterController.
    // Los errores 400 por campo se trasladarán al BindingResult con rejectValue.

    redirect.addFlashAttribute("toast",
        Toast.exito("Creamos tu cuenta. Ingresá con tu email y contraseña."));
    return "redirect:/login?registroExitoso=true";
  }
}
