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
        String accessToken = (String) response.getBody().get("access_token");
        session.setAttribute("JWT_TOKEN", accessToken);

        // Decodificamos el Payload del JWT (es la segunda parte del string separada por puntos)
        String[] chunks = accessToken.split("\\.");
        if (chunks.length > 1) {
          String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
          java.util.Map<String, Object> payloadMap = new com.fasterxml.jackson.databind.ObjectMapper().readValue(payload, java.util.Map.class);

          // Buscamos los roles inyectados por Keycloak
          java.util.Map<String, Object> realmAccess = (java.util.Map<String, Object>) payloadMap.get("realm_access");
          if (realmAccess != null && realmAccess.containsKey("roles")) {
            java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");

            // Redirigimos según el rol
            if (roles.contains("ADMIN")) {
              return "redirect:/dashboard/staff";
            } else if (roles.contains("ENTIDAD")) {
              return "redirect:/dashboard/entidad";
            }
          }
        }

        // Si no es admin ni entidad, asumimos que es el Donante
        return "redirect:/dashboard/donante";
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
