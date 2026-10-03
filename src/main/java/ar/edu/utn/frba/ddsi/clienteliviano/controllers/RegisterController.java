package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonanteCreateRequest;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.EntidadCreateRequest;
import ar.edu.utn.frba.ddsi.clienteliviano.services.RegistroService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import ar.edu.utn.frba.ddsi.clienteliviano.models.Toast;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {

  private final RestTemplate restTemplate;
  private final RegistroService registroService;

  @Value("${backend.api.url.donaciones}")
  private String backendApiUrl;

  @GetMapping("/donante")
  public String mostrarFormularioRegistro() {
    return "redirect:/registro";
  }

  @GetMapping("/entidad")
  public String mostrarFormularioEntidad() {
    return "redirect:/registro";
  }

  @PostMapping("/donante")
  public String registrarDonante(@ModelAttribute("donante") DonanteCreateRequest donanteRequest,
                                 RedirectAttributes redirect) {
    try {
      boolean keycloakCreado = registroService.registrarYAsignarRolEnKeycloak(
          donanteRequest.getEmail(), donanteRequest.getPassword(), "DONANTE",
          donanteRequest.getNombre(), donanteRequest.getApellido());

      if (!keycloakCreado) {
        redirect.addFlashAttribute("toast", Toast.error("No se pudo crear el usuario en el sistema de autenticación."));
        return "redirect:/registro";
      }

      String url = backendApiUrl + "/donantes";
      ResponseEntity<Void> response = restTemplate.postForEntity(url, donanteRequest, Void.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        redirect.addFlashAttribute("toast", Toast.exito("Cuenta creada con éxito."));
        return "redirect:/login?registroExitoso=true";
      } else {
        redirect.addFlashAttribute("toast", Toast.error("Error inesperado al intentar registrar el usuario en el backend."));
        return "redirect:/registro";
      }
    } catch (HttpClientErrorException.Conflict e) {
      redirect.addFlashAttribute("toast", Toast.error("Ya existe una cuenta con el correo: " + donanteRequest.getEmail()));
      return "redirect:/registro";
    } catch (HttpClientErrorException e) {
      redirect.addFlashAttribute("toast", Toast.error("Datos inválidos: " + e.getResponseBodyAsString()));
      return "redirect:/registro";
    } catch (Exception e) {
      redirect.addFlashAttribute("toast", Toast.error("No se pudo conectar con el servidor: " + e.getMessage()));
      return "redirect:/registro";
    }
  }

  @PostMapping("/entidad")
  public String registrarEntidad(@ModelAttribute("entidad") EntidadCreateRequest entidadRequest,
                                 RedirectAttributes redirect) {
    try {
      boolean keycloakCreado = registroService.registrarYAsignarRolEnKeycloak(
          entidadRequest.getEmail(), entidadRequest.getPassword(), "ENTIDAD",
          entidadRequest.getRazonSocial(), entidadRequest.getDireccion());

      if (!keycloakCreado) {
        redirect.addFlashAttribute("toast", Toast.error("No se pudo crear el usuario en el sistema de autenticación."));
        return "redirect:/registro";
      }

      String url = backendApiUrl + "/entidades";
      ResponseEntity<Void> response = restTemplate.postForEntity(url, entidadRequest, Void.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        redirect.addFlashAttribute("toast", Toast.exito("Entidad creada con éxito."));
        return "redirect:/login?registroExitoso=true";
      } else {
        redirect.addFlashAttribute("toast", Toast.error("Error inesperado al registrar la entidad."));
        return "redirect:/registro";
      }
    } catch (HttpClientErrorException.Conflict e) {
      redirect.addFlashAttribute("toast", Toast.error("Ya existe una cuenta con el correo: " + entidadRequest.getEmail()));
      return "redirect:/registro";
    } catch (HttpClientErrorException e) {
      redirect.addFlashAttribute("toast", Toast.error("Datos inválidos: " + e.getResponseBodyAsString()));
      return "redirect:/registro";
    } catch (Exception e) {
      redirect.addFlashAttribute("toast", Toast.error("Error de conexión con el servidor: " + e.getMessage()));
      return "redirect:/registro";
    }
  }
}