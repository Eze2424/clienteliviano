package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.LoginRequest;
import ar.edu.utn.frba.ddsi.clienteliviano.web.Sesion;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Controller
public class LoginController {

  private final Sesion sesion;
  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${keycloak.base-url:http://localhost:8085}")
  private String keycloakBaseUrl;

  @Value("${keycloak.realm:DonaTrack}")
  private String keycloakRealm;

  @Value("${keycloak.client-id:donatrack-client}")
  private String clientId;

  @Value("${backend.api.url.donaciones:http://localhost:8080/donaciones-service}")
  private String donacionesBaseUrl;

  public LoginController(Sesion sesion) {
    this.sesion = sesion;
  }

  @GetMapping("/login")
  public String getLogin() {
    return "publico/login";
  }

  @PostMapping("/login")
  public String login(LoginRequest loginRequest, HttpSession session, Model model) {
    String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakBaseUrl, keycloakRealm);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", clientId);
    body.add("grant_type", "password");
    body.add("username", loginRequest.username());
    body.add("password", loginRequest.password());

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        String accessToken = (String) response.getBody().get("access_token");
        session.setAttribute("JWT_TOKEN", accessToken);

        // 1. Decodificar Payload de Keycloak
        String[] parts = accessToken.split("\\.");
        if (parts.length > 1) {
          String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
          Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

          String email = (String) claims.get("email");
          if (email == null) {
            email = (String) claims.get("preferred_username");
          }
          String nombre = (String) claims.get("name");
          if (nombre == null) {
            nombre = email;
          }

          // 2. Extraer Rol
          Rol rol = Rol.DONANTE;
          String destino = "/donante/dashboard";

          Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
          if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            if (roles.contains("ADMIN")) {
              rol = Rol.ADMIN;
              destino = "/staff/dashboard";
            } else if (roles.contains("ENTIDAD")) {
              rol = Rol.ENTIDAD;
              destino = "/entidad/dashboard";
            }
          }

          // 3. Resolución dinámica del ID del donante desde MySQL
          Long idUsuario = 1L; // Fallback por defecto
          if (rol == Rol.DONANTE) {
            try {
              HttpHeaders authHeaders = new HttpHeaders();
              authHeaders.setBearerAuth(accessToken);
              HttpEntity<Void> entity = new HttpEntity<>(authHeaders);

              String url = donacionesBaseUrl + "/donantes/by-email?email=" + email;
              ResponseEntity<Map> resDonante = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

              if (resDonante.getStatusCode().is2xxSuccessful() && resDonante.getBody() != null) {
                Object rawId = resDonante.getBody().get("donanteId");
                if (rawId == null) {
                  rawId = resDonante.getBody().get("id");
                }
                if (rawId instanceof Number numId) {
                  idUsuario = numId.longValue();
                }
              }
            } catch (Exception ex) {
              System.err.println("No se pudo obtener el ID del donante por email: " + ex.getMessage());
            }
          }

          // 4. Crear UsuarioActual con el ID exacto que espera MySQL e Incentivos
          UsuarioActual usuarioActual = new UsuarioActual(idUsuario, nombre, email, rol);
          session.setAttribute("usuarioActual", usuarioActual);
          session.setAttribute("USUARIO", usuarioActual);

          return "redirect:" + destino;
        }
      }
    } catch (HttpClientErrorException e) {
      model.addAttribute("error", "Email o contraseña incorrectos.");
      return "publico/login";
    } catch (Exception e) {
      model.addAttribute("error", "Error de conexión con Keycloak: " + e.getMessage());
      return "publico/login";
    }

    model.addAttribute("error", "No se pudo iniciar sesión.");
    return "publico/login";
  }

  @PostMapping("/logout")
  public String logout(HttpSession session) {
    session.invalidate();
    return "redirect:/login?logout=true";
  }
}