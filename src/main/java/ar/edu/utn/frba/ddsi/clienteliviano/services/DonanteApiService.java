package ar.edu.utn.frba.ddsi.clienteliviano.services;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DashboardDonanteResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionResumenDTO;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.EntidadResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.RankingMensualResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.BienDetalleVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.EntidadVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.RankingFila;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.SubdonacionVista;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

  @Value("${backend.api.url.incentivos:http://localhost:8082/incentivos-service}")
  private String incentivosUrl;

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

  /**
   * Obtiene las entidades beneficiarias registradas, mapeadas a EntidadVista
   * y opcionalmente filtradas por nombre o dirección.
   */
  public List<EntidadVista> obtenerEntidades(String nombreFiltro) {
    try {
      ResponseEntity<EntidadResponse[]> respuesta = restTemplate.getForEntity(
          backendApiUrl + "/entidades", EntidadResponse[].class);
      if (!respuesta.getStatusCode().is2xxSuccessful() || respuesta.getBody() == null) {
        return List.of();
      }

      List<EntidadResponse> todas = List.of(respuesta.getBody());
      return todas.stream()
          .filter(e -> coincideFiltroNombre(e, nombreFiltro))
          .map(this::aVista)
          .toList();
    } catch (Exception e) {
      log.warn("No se pudieron obtener las entidades beneficiarias desde backend ({}): {}", backendApiUrl, e.getMessage());
      return List.of();
    }
  }

  private boolean coincideFiltroNombre(EntidadResponse e, String nombreFiltro) {
    if (nombreFiltro == null || nombreFiltro.isBlank()) {
      return true;
    }
    String query = nombreFiltro.trim().toLowerCase();
    boolean coincideRazonSocial = e.razonSocial() != null && e.razonSocial().toLowerCase().contains(query);
    boolean coincideDireccion = e.direccion() != null && e.direccion().toLowerCase().contains(query);
    return coincideRazonSocial || coincideDireccion;
  }

  public EntidadVista aVista(EntidadResponse entidad) {
    List<NecesidadResponse> nec = entidad.necesidades() != null
        ? entidad.necesidades().stream()
            .map(n -> new NecesidadResponse(
                n.id(),
                (n.subcategoria() != null && !n.subcategoria().isBlank()) ? n.subcategoria() : "General",
                n.descripcion(),
                n.cantidadSolicitada() != null ? n.cantidadSolicitada() : 0.0,
                Boolean.TRUE.equals(n.esExtraordinaria())
            ))
            .toList()
        : List.of();

    boolean extraordinaria = nec.stream().anyMatch(n -> Boolean.TRUE.equals(n.esExtraordinaria()));
    String tipo = inferirTipoEntidad(entidad.razonSocial());

    return new EntidadVista(
        entidad.id(),
        entidad.razonSocial(),
        entidad.direccion(),
        entidad.telefono(),
        entidad.latitud(),
        entidad.longitud(),
        tipo,
        extraordinaria,
        null, // foto
        entidad.razonSocial(),
        nec
    );
  }

  private String inferirTipoEntidad(String razonSocial) {
    if (razonSocial == null) return "Entidad Beneficiaria";
    String rz = razonSocial.toLowerCase();
    if (rz.contains("comedor")) return "Comedor";
    if (rz.contains("hogar")) return "Hogar";
    if (rz.contains("escuela")) return "Escuela";
    if (rz.contains("fundaci")) return "Fundación";
    if (rz.contains("asociaci")) return "Asociación";
    if (rz.contains("coop")) return "Cooperativa";
    return "Organización";
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

  /* =========================================================
     Módulo Ranking (incentivos-service)
     ========================================================= */

  public List<String> obtenerPeriodosRanking() {
    try {
      var respuesta = restTemplate.exchange(
          incentivosUrl + "/ranking/periodos",
          HttpMethod.GET,
          null,
          new ParameterizedTypeReference<List<String>>() {}
      );
      if (respuesta.getStatusCode().is2xxSuccessful() && respuesta.getBody() != null && !respuesta.getBody().isEmpty()) {
        return respuesta.getBody();
      }
      return List.of(YearMonth.now().toString());
    } catch (Exception e) {
      log.warn("Error al listar periodos de ranking desde {}: {}", incentivosUrl, e.getMessage());
      return List.of(YearMonth.now().toString());
    }
  }

  public List<RankingFila> obtenerRanking(String periodo, int top) {
    try {
      var uriBuilder = UriComponentsBuilder.fromUriString(incentivosUrl + "/ranking")
          .queryParam("top", top > 0 ? top : 10);
      if (periodo != null && !periodo.isBlank()) {
        uriBuilder.queryParam("periodo", periodo.trim());
      }
      var uri = uriBuilder.toUriString();

      var respuesta = restTemplate.exchange(
          uri,
          HttpMethod.GET,
          null,
          new ParameterizedTypeReference<List<RankingMensualResponse>>() {}
      );

      if (respuesta.getStatusCode().is2xxSuccessful() && respuesta.getBody() != null) {
        List<RankingFila> filas = new ArrayList<>();
        for (var r : respuesta.getBody()) {
          int pos = r.posicion() != null ? r.posicion() : (filas.size() + 1);
          long misiones = r.misionesCumplidas() != null ? r.misionesCumplidas() : 0L;
          int insignias = r.insignias() != null ? r.insignias() : 0;
          String tipo = r.tipoDonante() != null ? r.tipoDonante() : "HUMANA";
          String cat = r.categoria() != null ? r.categoria() : "Colaborador";
          long impacto = (misiones * 100) + (insignias * 50);

          filas.add(new RankingFila(
              pos,
              r.idDonante(),
              r.nombre(),
              tipo,
              cat,
              misiones,
              insignias,
              impacto
          ));
        }
        return filas;
      }
      return List.of();
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al obtener ranking desde {}: {} - {}", incentivosUrl, e.getStatusCode(), e.getResponseBodyAsString());
      return List.of();
    } catch (Exception e) {
      log.error("Error al obtener ranking desde {}: {}", incentivosUrl, e.getMessage());
      return List.of();
    }
  }
}
