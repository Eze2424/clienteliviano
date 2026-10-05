package ar.edu.utn.frba.ddsi.clienteliviano.services;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DashboardDonanteResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionResumenDTO;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.BienDetalleVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.SubdonacionVista;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger log = LoggerFactory.getLogger(DonanteApiService.class);
  private static final DateTimeFormatter FMT_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
      log.warn("No se pudo obtener el dashboard del donante desde backend ({}): {}", backendApiUrl, e.getMessage());
      return Optional.empty();
    }
  }

  /** Traduce la forma de la API a la que necesita la vista. */
  public List<DonacionVista> aVista(List<DonacionResumenDTO> resumenes) {
    return resumenes == null ? List.of() : resumenes.stream().map(d -> {
      List<SubdonacionVista> subVistas = new ArrayList<>();
      int totalCantidadBienes = 0;

      if (d.getSubdonaciones() != null) {
        for (var sub : d.getSubdonaciones()) {
          List<BienDetalleVista> bienesVistas = new ArrayList<>();
          int cantSub = 0;
          List<String> descripciones = new ArrayList<>();

          if (sub.getBienes() != null) {
            for (var b : sub.getBienes()) {
              bienesVistas.add(new BienDetalleVista(
                  b.getId(),
                  b.getDescripcion(),
                  b.getCantidad(),
                  b.isEsUsado(),
                  b.getFechaDeVencimiento(),
                  b.getPeso(),
                  b.getVolumen(),
                  b.getFoto()
              ));
              cantSub += (int) b.getCantidad();
              if (b.getDescripcion() != null && !b.getDescripcion().isBlank()) {
                descripciones.add((int) b.getCantidad() > 1
                    ? (int) b.getCantidad() + " " + b.getDescripcion()
                    : b.getDescripcion());
              }
            }
          }
          totalCantidadBienes += cantSub > 0 ? cantSub : (sub.getBienes() != null ? sub.getBienes().size() : 0);
          String bienesResumen = String.join(", ", descripciones);

          subVistas.add(new SubdonacionVista(
              sub.getId(),
              sub.getSubcategoria() != null ? sub.getSubcategoria() : "General",
              sub.getCategoria(),
              sub.getEstado() != null ? sub.getEstado() : "EN_DEPOSITO",
              sub.getEntidadNombre() != null ? sub.getEntidadNombre() : "Sin asignar",
              sub.getEntidadId(),
              sub.getFechaEntrega(),
              cantSub > 0 ? cantSub : (sub.getBienes() != null ? sub.getBienes().size() : 0),
              bienesResumen,
              bienesVistas
          ));
        }
      }

      return new DonacionVista(
          d.getId(),
          d.getDescripcionBreve() != null ? d.getDescripcionBreve() : d.getTitulo(),
          d.getEstado(),
          parsearFecha(d.getFecha()),
          null,
          false,
          null,
          null,
          (d.getEntidadNombre() != null && !d.getEntidadNombre().isBlank() && !d.getEntidadNombre().equalsIgnoreCase("Sin asignar")) ? 1L : null,
          d.getEntidadNombre(),
          totalCantidadBienes > 0 ? totalCantidadBienes : (subVistas.size()),
          d.getImagenUrl(),
          subVistas
      );
    }).toList();
  }

  private LocalDate parsearFecha(String fecha) {
    if (fecha == null || fecha.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(fecha, FMT_DD_MM_YYYY);
    } catch (DateTimeParseException e) {
      try {
        return LocalDate.parse(fecha);
      } catch (DateTimeParseException e2) {
        return null;
      }
    }
  }
}
