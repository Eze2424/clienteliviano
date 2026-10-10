package ar.edu.utn.frba.ddsi.clienteliviano.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestClientConfig {

  private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

  @Value("${keycloak.base-url:http://localhost:8085}")
  private String keycloakBaseUrl;

  @Value("${keycloak.realm:DonaTrack}")
  private String keycloakRealm;

  @Value("${keycloak.client-id:donatrack-client}")
  private String clientId;

  @Bean
  public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate(new org.springframework.http.client.JdkClientHttpRequestFactory());

    ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest httpRequest = attributes.getRequest();
        HttpSession session = httpRequest.getSession(false);

        if (session != null) {
          String token = obtenerTokenValido(session);
          if (token != null) {
            request.getHeaders().setBearerAuth(token);
          }
        }
      }
      return execution.execute(request, body);
    };

    restTemplate.getInterceptors().add(interceptor);
    return restTemplate;
  }

  private synchronized String obtenerTokenValido(HttpSession session) {
    String currentToken = (String) session.getAttribute("JWT_TOKEN");
    String refreshToken = (String) session.getAttribute("REFRESH_TOKEN");
    Long expiresAt = (Long) session.getAttribute("JWT_EXPIRES_AT");

    if (currentToken == null) {
      return null;
    }

    // Si aún no está por vencer (margen de 30 segundos), usar el token actual
    if (expiresAt != null && System.currentTimeMillis() < (expiresAt - 30000L)) {
      return currentToken;
    }

    // Si no hay refresh token disponible, devolver el token actual
    if (refreshToken == null) {
      return currentToken;
    }

    // Renovar con Keycloak
    try {
      String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakBaseUrl, keycloakRealm);
      RestTemplate tokenClient = new RestTemplate();

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> formBody = new LinkedMultiValueMap<>();
      formBody.add("client_id", clientId);
      formBody.add("grant_type", "refresh_token");
      formBody.add("refresh_token", refreshToken);

      HttpEntity<MultiValueMap<String, String>> refreshReq = new HttpEntity<>(formBody, headers);
      ResponseEntity<Map> response = tokenClient.postForEntity(tokenEndpoint, refreshReq, Map.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        String newAccess = (String) response.getBody().get("access_token");
        String newRefresh = (String) response.getBody().get("refresh_token");
        Object newExpires = response.getBody().get("expires_in");

        session.setAttribute("JWT_TOKEN", newAccess);
        if (newRefresh != null) {
          session.setAttribute("REFRESH_TOKEN", newRefresh);
        }
        if (newExpires instanceof Number num) {
          session.setAttribute("JWT_EXPIRES_AT", System.currentTimeMillis() + (num.longValue() * 1000L));
        }
        log.info("Token JWT renovado exitosamente para la sesión activa.");
        return newAccess;
      }
    } catch (Exception e) {
      log.warn("No se pudo renovar el token JWT con Keycloak: {}", e.getMessage());
    }

    return currentToken;
  }
}