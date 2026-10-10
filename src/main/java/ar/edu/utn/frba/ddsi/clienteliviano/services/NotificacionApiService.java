package ar.edu.utn.frba.ddsi.clienteliviano.services;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NotificacionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.NotificacionVista;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Servicio cliente para interactuar con notificaciones-service.
 * Permite listar las notificaciones de un usuario por email/id y marcarlas como leídas.
 */
@Service
public class NotificacionApiService {

  private static final Logger log = LoggerFactory.getLogger(NotificacionApiService.class);

  private final RestTemplate restTemplate;

  @Value("${backend.api.url.notificaciones}")
  private String notificacionesUrl;

  public NotificacionApiService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * Consulta las notificaciones enviadas al email y/o destinatarioId dados.
   * Aplica URL-encoding al email para soportar caracteres especiales sin doble encoding.
   * Ante cualquier falla de red devuelve una lista vacía y registra un warning.
   */
  public List<NotificacionResponse> listar(String email, Long destinatarioId) {
    if ((email == null || email.isBlank()) && destinatarioId == null) {
      return Collections.emptyList();
    }
    try {
      StringBuilder urlBuilder = new StringBuilder(notificacionesUrl);
      boolean firstParam = true;
      if (email != null && !email.isBlank()) {
        urlBuilder.append("?email=").append(URLEncoder.encode(email, StandardCharsets.UTF_8));
        firstParam = false;
      }
      if (destinatarioId != null) {
        urlBuilder.append(firstParam ? "?" : "&").append("destinatarioId=").append(destinatarioId);
      }

      URI uri = URI.create(urlBuilder.toString());
      var respuesta = restTemplate.getForEntity(uri, NotificacionResponse[].class);
      if (respuesta.getStatusCode().is2xxSuccessful() && respuesta.getBody() != null) {
        return Arrays.asList(respuesta.getBody());
      }
      return Collections.emptyList();
    } catch (Exception e) {
      log.warn("Error al consultar notificaciones en {}: {}", notificacionesUrl, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Marca una notificación como leída enviando PATCH {notificaciones}/{id}/leida.
   * Devuelve true si la respuesta es 2xx, false en caso de error.
   */
  public boolean marcarLeida(Long id) {
    if (id == null) {
      return false;
    }
    try {
      String url = notificacionesUrl + "/" + id + "/leida";
      var respuesta = restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity.EMPTY, Void.class);
      return respuesta.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      log.warn("Error al marcar como leída la notificación #{} en {}: {}", id, notificacionesUrl, e.getMessage());
      return false;
    }
  }

  /**
   * Mapea el DTO de respuesta a la vista NotificacionVista.
   */
  public NotificacionVista mapear(NotificacionResponse dto) {
    if (dto == null) {
      return null;
    }
    return new NotificacionVista(
        dto.id(),
        dto.tipo(),
        dto.titulo(),
        dto.detalle(),
        dto.fecha(),
        dto.leida()
    );
  }
}
