package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.LoginRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Controller
public class LoginController {

  private final RestTemplate restTemplate = new RestTemplate();

  // La URL de tu Keycloak (ajustar puerto 8085 si lo cambiaste)
  @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri:http://localhost:8085/realms/DonaTrack}")
  private String keycloakIssuerUri;

  @GetMapping("/login")
  public String mostrarLogin() {
    return "login"; //ACA VA EL NOMBRE DEL HTML Q USEMOS PARA LOGIN
  }

  @PostMapping("/login")
  public String procesarLogin(LoginRequest loginRequest, Model model, HttpSession session) {

    String tokenEndpoint = keycloakIssuerUri + "/protocol/openid-connect/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    // Armamos el body idéntico a lo que mandabas en Postman
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("client_id", "donatrack-client");
    map.add("grant_type", "password");
    map.add("username", loginRequest.username());
    map.add("password", loginRequest.password());

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

    try {
      // Le pegamos a Keycloak por detrás
      ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        // Obtenemos el access_token enorme
        String accessToken = (String) response.getBody().get("access_token");

        // Lo guardamos en la sesión local del cliente liviano
        session.setAttribute("JWT_TOKEN", accessToken);

        return "redirect:/home"; // Login exitoso, va a la home
      }
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      System.err.println("Rechazo de Keycloak: " + e.getResponseBodyAsString());
      model.addAttribute("error", "Credenciales inválidas");
      return "login";
    } catch (Exception e) {
      System.err.println("Error de conexión: " + e.getMessage());
      model.addAttribute("error", "Ocurrió un error al contactar al servidor");
      return "login";
    }

    model.addAttribute("error", "Ocurrió un error al contactar al servidor de autenticación");
    return "login";
  }
}
