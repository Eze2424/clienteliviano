package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonanteRequest;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.EntidadCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

  @Value("${backend.api.url.donaciones}")
  private String backendApiUrl;

  @GetMapping("/donante")
  public String mostrarFormularioRegistro(Model model) {
    model.addAttribute("donante", new DonanteRequest());
    return "registro-donante"; // vistas
  }
  @GetMapping("/entidad")
  public String mostrarFormularioEntidad(Model model) {

    model.addAttribute("entidad", new EntidadCreateRequest());
    return "registro-entidad";
  }

  @PostMapping("/donante")
  public String registrarDonante(@ModelAttribute("donante") DonanteRequest donanteRequest, Model model) {
    try {
      String url = backendApiUrl + "/donantes";

      ResponseEntity<Void> response = restTemplate.postForEntity(
          url,
          donanteRequest,
          Void.class
      );

      if (response.getStatusCode().is2xxSuccessful()) {
        return "redirect:/login?registroExitoso=true";
      } else {
        model.addAttribute("error", "Error inesperado al intentar registrar el usuario.");
        return "registro-donante";
      }

    } catch (HttpClientErrorException e) {
      model.addAttribute("error", "Datos inválidos: " + e.getResponseBodyAsString());
      return "registro-donante";
    } catch (Exception e) {
      model.addAttribute("error", "No se pudo conectar con el servidor: " + e.getMessage());
      return "registro-donante";
    }
  }

  @PostMapping("/entidad")
  public String registrarEntidad(@ModelAttribute("entidad") EntidadCreateRequest entidadRequest, Model model) {
    try {

      String url = backendApiUrl + "/entidades";

      ResponseEntity<Void> response = restTemplate.postForEntity(
          url,
          entidadRequest,
          Void.class
      );

      if (response.getStatusCode().is2xxSuccessful()) {
        return "redirect:/login?registroExitoso=true";
      } else {
        model.addAttribute("error", "Error inesperado al registrar la entidad.");
        return "registro-entidad";
      }

    } catch (HttpClientErrorException e) {
      model.addAttribute("error", "Datos inválidos: " + e.getResponseBodyAsString());
      return "registro-entidad";
    } catch (Exception e) {
      model.addAttribute("error", "Error de conexión con el servidor: " + e.getMessage());
      return "registro-entidad";
    }
  }
}