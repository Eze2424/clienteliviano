package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DashboardDonanteResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

@Controller
public class DashboardController {

  private final RestTemplate restTemplate;
  @Value("${backend.api.url.donaciones}")
  private String backendApiUrl;
  public DashboardController(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  // Dashboard exclusivo para el Donante
  @GetMapping("/dashboard/donante")
  public String mostrarDashboardDonante(Model model, HttpSession session) {
    if (session.getAttribute("JWT_TOKEN") == null) {
      return "redirect:/login";
    }

    try {
      String url = backendApiUrl + "/donantes/me/dashboard";
      ResponseEntity<DashboardDonanteResponse> response = restTemplate.getForEntity(url, DashboardDonanteResponse.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        model.addAttribute("dashboard", response.getBody());
      }
    } catch (Exception e) {
      // Fallback en caso de que el backend falle
      model.addAttribute("dashboard", new DashboardDonanteResponse());
    }

    return "donante-dashboard";
  }

  // Dashboard exclusivo para el Staff / Admin
  @GetMapping("/dashboard/staff")
  public String mostrarDashboardStaff(Model model) {
    // Acá luego le podés pegar a Logística para ver los camiones
    return "staff-dashboard"; // Levanta tu staff-dashboard.html
  }

  // Dashboard exclusivo para las Entidades
  @GetMapping("/dashboard/entidad")
  public String mostrarDashboardEntidad(Model model) {
    return "donante-entidades"; // O el HTML que hayan armado para la Entidad
  }
}