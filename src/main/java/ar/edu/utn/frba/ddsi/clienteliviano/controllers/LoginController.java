package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.LoginRequest;
import ar.edu.utn.frba.ddsi.clienteliviano.web.Sesion;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Autenticacion contra Keycloak por el grant `password`.
 *
 * El token y la identidad quedan en la sesion del servidor: nunca llegan al
 * navegador. RestClientConfig lee el token de esa misma sesion para firmar
 * cada llamada saliente a la API.
 */
@Controller
public class LoginController {

  private final RestTemplate restTemplate;
  private final Sesion sesion;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
  private String keycloakIssuerUri;

  @Value("${keycloak.client-id}")
  private String clientId;

  public LoginController(RestTemplate restTemplate, Sesion sesion) {
    this.restTemplate = restTemplate;
    this.sesion = sesion;
  }

  @GetMapping("/login")
  public String mostrarLogin() {
    return "publico/login";
  }

  @PostMapping("/login")
  public String procesarLogin(LoginRequest loginRequest, Model model, HttpSession session) {
    String tokenEndpoint = keycloakIssuerUri + "/protocol/openid-connect/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", clientId);
    body.add("grant_type", "password");
    body.add("username", loginRequest.username());
    body.add("password", loginRequest.password());

    try {
      ResponseEntity<Map> respuesta =
          restTemplate.postForEntity(tokenEndpoint, new HttpEntity<>(body, headers), Map.class);

      if (respuesta.getStatusCode().is2xxSuccessful() && respuesta.getBody() != null) {
        String accessToken = (String) respuesta.getBody().get("access_token");
        session.setAttribute(Sesion.TOKEN, accessToken);

        UsuarioActual usuario = leerUsuarioDelToken(accessToken, loginRequest.username());
        sesion.iniciar(session, usuario);

        // Cada rol entra a su propia area.
        return "redirect:" + usuario.rol().getInicio();
      }
    } catch (HttpClientErrorException e) {
      model.addAttribute("error", "Email o contraseña incorrectos. Revisalos y probá de nuevo.");
      return "publico/login";
    } catch (Exception e) {
      model.addAttribute("error",
          "No pudimos contactar al servicio de autenticación. Probá de nuevo en unos minutos.");
      return "publico/login";
    }

    model.addAttribute("error",
        "No pudimos contactar al servicio de autenticación. Probá de nuevo en unos minutos.");
    return "publico/login";
  }

  /**
   * Lee el payload del JWT para saber quien entro y con que rol.
   *
   * El rol lo decide Keycloak, no el usuario: viene en realm_access.roles, el
   * mismo claim que leen los cuatro microservicios para autorizar. Solo se
   * decodifica el payload para mostrar nombre y rutear; la validacion de la
   * firma es responsabilidad de la API, que es quien confia en el token.
   */
  private UsuarioActual leerUsuarioDelToken(String accessToken, String emailIngresado) {
    String nombre = emailIngresado;
    Rol rol = Rol.DONANTE;

    try {
      String[] partes = accessToken.split("\\.");
      if (partes.length > 1) {
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        Map<String, Object> claims = objectMapper.readValue(payload, Map.class);

        if (claims.get("name") instanceof String n && !n.isBlank()) {
          nombre = n;
        } else if (claims.get("preferred_username") instanceof String u && !u.isBlank()) {
          nombre = u;
        }

        if (claims.get("realm_access") instanceof Map<?, ?> realmAccess
            && realmAccess.get("roles") instanceof List<?> roles) {
          // Si alguien tuviera varios roles, gana el de mas alcance.
          if (roles.contains("ADMIN")) {
            rol = Rol.ADMIN;
          } else if (roles.contains("ENTIDAD")) {
            rol = Rol.ENTIDAD;
          }
        }
      }
    } catch (Exception e) {
      // Un token que no podemos leer no debe tumbar el login: entra como
      // donante, que es el rol de menor alcance.
      System.err.println("No se pudo leer el payload del JWT: " + e.getMessage());
    }

    return new UsuarioActual(null, nombre, emailIngresado, rol);
  }
}
