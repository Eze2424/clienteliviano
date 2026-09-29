package ar.edu.utn.frba.ddsi.clienteliviano.services;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DashboardDonanteResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionResumenDTO;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Servicio cliente del donante: aisla el consumo HTTP para que los controllers
 * queden delgados. El token lo agrega el interceptor de RestClientConfig.
 *
 * Toda falla de la API se traduce a Optional.empty(): el controller decide que
 * mostrar. Nunca sale una excepcion hacia la vista.
 */
@Service
public class DonanteApiService {

  private final RestTemplate restTemplate;

  @Value("${backend.api.url.donaciones}")
  private String backendApiUrl;

  public DonanteApiService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * Resumen del dashboard del donante logueado.
   * Endpoint agregado que propuso el equipo: resuelve las brechas G1 y G2 en
   * una sola llamada, sin que el cliente tenga que averiguar el donanteId.
   */
  public Optional<DashboardDonanteResponse> dashboard() {
    try {
      var respuesta = restTemplate.getForEntity(
          backendApiUrl + "/donantes/me/dashboard", DashboardDonanteResponse.class);
      return respuesta.getStatusCode().is2xxSuccessful()
          ? Optional.ofNullable(respuesta.getBody())
          : Optional.empty();
    } catch (Exception e) {
      // La API no respondio o todavia no expone el endpoint. El controller
      // decide el plan B; el usuario nunca ve un stack trace.
      return Optional.empty();
    }
  }

  /** Traduce la forma de la API a la que necesita la vista. */
  public List<DonacionVista> aVista(List<DonacionResumenDTO> resumenes) {
    return resumenes == null ? List.of() : resumenes.stream().map(d -> new DonacionVista(
        d.getId(),
        d.getDescripcionBreve() != null ? d.getDescripcionBreve() : d.getTitulo(),
        d.getEstado(),
        parsearFecha(d.getFecha()),
        null,
        false,
        null,
        null,
        null,
        d.getEntidadNombre(),
        0,
        d.getImagenUrl()
    )).toList();
  }

  private LocalDate parsearFecha(String fecha) {
    try {
      return fecha == null ? null : LocalDate.parse(fecha);
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
