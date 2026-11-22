package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.Toast;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionPublicaResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.RegistroForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.entities.TipoOrganizacion;
import ar.edu.utn.frba.ddsi.clienteliviano.services.RegistroService;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Vistas abiertas: portada, alta de cuenta y textos legales.
 */
@Controller
public class PublicoController {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PublicoController.class);

  private final RegistroService registroService;
  private final RestTemplate restTemplate;

  @Value("${backend.api.url.donaciones:http://localhost:8080/donaciones-service}")
  private String donacionesUrl;

  @org.springframework.beans.factory.annotation.Autowired
  public PublicoController(RegistroService registroService, RestTemplate restTemplate) {
    this.registroService = registroService;
    this.restTemplate = restTemplate;
  }

  public PublicoController(RegistroService registroService) {
    this(registroService, new RestTemplate());
  }

  @GetMapping("/")
  public String portada(Model model) {
    List<DonacionPublicaResponse> entregasPublicas = List.of();
    try {
      if (restTemplate != null && donacionesUrl != null) {
        var resp = restTemplate.getForEntity(donacionesUrl + "/publico/entregadas", DonacionPublicaResponse[].class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
          entregasPublicas = Arrays.asList(resp.getBody());
        }
      }
    } catch (Exception e) {
      log.warn("No se pudieron obtener las entregas públicas desde donaciones-service: {}", e.getMessage(), e);
      entregasPublicas = List.of();
    }
    model.addAttribute("entregasPublicas", entregasPublicas);
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

    try {
      registroService.procesarRegistro(form);
      redirect.addFlashAttribute("toast",
          Toast.exito("Creamos tu cuenta exitosamente. Ingresá con tu email y contraseña."));
      return "redirect:/login?registroExitoso=true";
    } catch (HttpClientErrorException.Conflict e) {
      model.addAttribute("tiposOrganizacion", TipoOrganizacion.values());
      model.addAttribute("error", "Ya existe un usuario registrado con el correo: " + form.getEmail());
      return "publico/registro";
    } catch (HttpClientErrorException e) {
      model.addAttribute("tiposOrganizacion", TipoOrganizacion.values());
      model.addAttribute("error", "Datos inválidos: " + e.getResponseBodyAsString());
      return "publico/registro";
    } catch (Exception e) {
      model.addAttribute("tiposOrganizacion", TipoOrganizacion.values());
      model.addAttribute("error", "Error al registrar la cuenta: " + e.getMessage());
      return "publico/registro";
    }
  }
}
