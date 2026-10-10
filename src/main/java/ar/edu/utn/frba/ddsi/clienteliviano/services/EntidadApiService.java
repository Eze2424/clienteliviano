package ar.edu.utn.frba.ddsi.clienteliviano.services;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.EntidadResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Servicio cliente de la entidad beneficiaria: aisla el consumo HTTP
 * de los controladores web de entidad.
 * El token JWT lo inyecta el interceptor de RestClientConfig.
 *
 * Las fallas de la API se traducen a Optional.empty() o respuestas de error controladas;
 * nunca sube una excepción de red no manejada a la vista.
 */
@Service
public class EntidadApiService {

  private static final Logger log = LoggerFactory.getLogger(EntidadApiService.class);

  private final RestTemplate restTemplate;

  @Value("${backend.api.url.donaciones}")
  private String backendApiUrl;

  @Value("${backend.api.url.logistica:http://localhost:8083}")
  private String logisticaUrl;

  public EntidadApiService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /**
   * Obtiene los datos de la entidad asociada al usuario logueado en la sesión.
   * Consume GET /donaciones-service/entidades/me
   */
  public Optional<EntidadResponse> miEntidad() {
    try {
      var respuesta = restTemplate.getForEntity(backendApiUrl + "/entidades/me", EntidadResponse.class);
      return respuesta.getStatusCode().is2xxSuccessful()
          ? Optional.ofNullable(respuesta.getBody())
          : Optional.empty();
    } catch (Exception e) {
      log.warn("No se pudo obtener la entidad logueada desde backend ({}): {}", backendApiUrl, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Catálogo de subcategorías disponibles para asociar a una necesidad.
   * Consume GET /donaciones-service/subcategorias
   */
  public java.util.List<ar.edu.utn.frba.ddsi.clienteliviano.models.dto.SubcategoriaResponse> subcategorias() {
    try {
      var respuesta = restTemplate.getForEntity(
          backendApiUrl + "/subcategorias",
          ar.edu.utn.frba.ddsi.clienteliviano.models.dto.SubcategoriaResponse[].class);
      if (respuesta.getStatusCode().is2xxSuccessful() && respuesta.getBody() != null) {
        return java.util.Arrays.asList(respuesta.getBody());
      }
      return java.util.List.of();
    } catch (Exception e) {
      log.warn("No se pudo obtener el catálogo de subcategorías ({}): {}", backendApiUrl, e.getMessage());
      return java.util.List.of();
    }
  }

  /**
   * Lista las necesidades materiales registradas por la entidad logueada.
   * Consume GET /donaciones-service/entidades/me/necesidades
   */
  public java.util.List<ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadResponse> necesidades() {
    try {
      var respuesta = restTemplate.getForEntity(
          backendApiUrl + "/entidades/me/necesidades",
          ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadResponse[].class);
      if (respuesta.getStatusCode().is2xxSuccessful() && respuesta.getBody() != null) {
        return java.util.Arrays.asList(respuesta.getBody());
      }
      return java.util.List.of();
    } catch (Exception e) {
      log.warn("No se pudieron obtener las necesidades de la entidad ({}): {}", backendApiUrl, e.getMessage());
      return java.util.List.of();
    }
  }

  /**
   * Registra una nueva necesidad material para la entidad.
   * Consume POST /donaciones-service/entidades/{entidadId}/necesidades
   */
  public ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion registrarNecesidad(
      Long entidadId, ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadForm form) {
    if (entidadId == null) {
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("No se pudo identificar la entidad logueada.");
    }
    if (form.getCantidadSolicitada() == null || form.getCantidadSolicitada() <= 0) {
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("La cantidad solicitada debe ser mayor a cero.");
    }
    if (form.getSubcategoriaId() == null) {
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("Debes seleccionar una subcategoría.");
    }

    var body = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadCreateRequest(
        form.getSubcategoriaId(),
        entidadId,
        form.getDescripcion() != null ? form.getDescripcion().trim() : "",
        form.getCantidadSolicitada(),
        form.isEsExtraordinaria()
    );

    try {
      var respuesta = restTemplate.postForEntity(
          backendApiUrl + "/entidades/" + entidadId + "/necesidades",
          body,
          ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadResponse.class
      );
      if (respuesta.getStatusCode().is2xxSuccessful()) {
        return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.ok("Necesidad registrada correctamente.");
      }
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("No se pudo registrar la necesidad.");
    } catch (org.springframework.web.client.HttpStatusCodeException e) {
      log.warn("Error HTTP al registrar necesidad: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      if (e.getStatusCode().value() == 403) {
        return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("No tienes permisos para modificar esta entidad.");
      }
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("Error al registrar necesidad: datos inválidos.");
    } catch (Exception e) {
      log.warn("Error de conexión al registrar necesidad: {}", e.getMessage());
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("No se pudo conectar con el servidor de donaciones.");
    }
  }

  /**
   * Elimina una necesidad de la entidad.
   * Consume DELETE /donaciones-service/entidades/{entidadId}/necesidades/{necesidadId}
   */
  public ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion eliminarNecesidad(Long entidadId, Long necesidadId) {
    if (entidadId == null || necesidadId == null) {
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("Identificadores inválidos.");
    }
    try {
      restTemplate.delete(backendApiUrl + "/entidades/" + entidadId + "/necesidades/" + necesidadId);
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.ok("Necesidad eliminada correctamente.");
    } catch (org.springframework.web.client.HttpStatusCodeException e) {
      log.warn("Error HTTP al eliminar necesidad: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      if (e.getStatusCode().value() == 403) {
        return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("No tienes permiso para dar de baja esta necesidad.");
      }
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("No se pudo eliminar la necesidad.");
    } catch (Exception e) {
      log.warn("Error de conexión al eliminar necesidad: {}", e.getMessage());
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("No se pudo conectar con el servidor de donaciones.");
    }
  }

  /**
   * Resumen y listado de donaciones asignadas a la entidad logueada.
   * Consume GET /donaciones-service/entidades/me/dashboard
   */
  public java.util.Optional<ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DashboardEntidadResponse> dashboard() {
    try {
      var respuesta = restTemplate.getForEntity(
          backendApiUrl + "/entidades/me/dashboard",
          ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DashboardEntidadResponse.class);
      return respuesta.getStatusCode().is2xxSuccessful()
          ? java.util.Optional.ofNullable(respuesta.getBody())
          : java.util.Optional.empty();
    } catch (Exception e) {
      log.warn("No se pudo obtener el dashboard de la entidad ({}): {}", backendApiUrl, e.getMessage());
      return java.util.Optional.empty();
    }
  }

  /**
   * Obtiene el detalle de una donación independiente.
   * Consume GET /donaciones-service/donaciones-independientes/{id}
   */
  public java.util.Optional<ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionDetalleDTO> donacion(Long id) {
    try {
      var respuesta = restTemplate.getForEntity(
          backendApiUrl + "/donaciones-independientes/" + id,
          ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionDetalleDTO.class);
      return respuesta.getStatusCode().is2xxSuccessful()
          ? java.util.Optional.ofNullable(respuesta.getBody())
          : java.util.Optional.empty();
    } catch (org.springframework.web.client.HttpStatusCodeException e) {
      log.warn("Error HTTP al obtener donación {}: {}", id, e.getStatusCode());
      return java.util.Optional.empty();
    } catch (Exception e) {
      log.warn("No se pudo obtener la donación {} ({}): {}", id, backendApiUrl, e.getMessage());
      return java.util.Optional.empty();
    }
  }

  public ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista mapearDonacion(
      ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionAsignadaDTO d, Long entidadId) {
    java.time.LocalDate hoy = java.time.LocalDate.now();
    return new ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista(
        d.id(),
        d.descripcion(),
        d.estado(),
        d.fecha() != null ? d.fecha() : hoy,
        d.vencimiento(),
        d.vencimiento() != null && d.vencimiento().isBefore(hoy),
        d.donanteId(),
        d.donanteNombre(),
        entidadId,
        null,
        d.cantidadBienes(),
        null,
        java.util.List.of()
    );
  }

  public ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista mapearDetalle(
      ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionDetalleDTO d) {
    java.time.LocalDate hoy = java.time.LocalDate.now();
    return new ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista(
        d.id(),
        d.descripcion(),
        d.estado(),
        d.fecha() != null ? d.fecha() : hoy,
        d.vencimiento(),
        d.vencimiento() != null && d.vencimiento().isBefore(hoy),
        d.donanteId(),
        d.donanteNombre(),
        d.entidadId(),
        d.entidadNombre(),
        d.cantidadBienes(),
        null,
        java.util.List.of()
    );
  }

  /**
   * Consulta el seguimiento de entregas para las donaciones dadas.
   * Consume GET {logistica}/entregas/por-donaciones?ids=...
   */
  public java.util.List<ar.edu.utn.frba.ddsi.clienteliviano.models.dto.EntregaSeguimientoDTO> entregas(java.util.List<Long> donacionIds) {
    if (donacionIds == null || donacionIds.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    try {
      String idsParam = donacionIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
      String url = logisticaUrl + "/entregas/por-donaciones?ids=" + idsParam;
      var respuesta = restTemplate.getForEntity(url, ar.edu.utn.frba.ddsi.clienteliviano.models.dto.EntregaSeguimientoDTO[].class);
      if (respuesta.getStatusCode().is2xxSuccessful() && respuesta.getBody() != null) {
        return java.util.Arrays.asList(respuesta.getBody());
      }
      return java.util.Collections.emptyList();
    } catch (Exception e) {
      log.warn("Error al consultar seguimiento de entregas en {}: {}", logisticaUrl, e.getMessage());
      return java.util.Collections.emptyList();
    }
  }

  /**
   * Mapea un EntregaSeguimientoDTO a EntregaVista para la pantalla de mapa/seguimiento.
   */
  public ar.edu.utn.frba.ddsi.clienteliviano.models.vista.EntregaVista mapearEntrega(
      ar.edu.utn.frba.ddsi.clienteliviano.models.dto.EntregaSeguimientoDTO dto, String descripcionDonacion) {
    if (dto == null) {
      return null;
    }
    double lat = dto.latitud() != null ? dto.latitud() : 0.0;
    double lon = dto.longitud() != null ? dto.longitud() : 0.0;
    return new ar.edu.utn.frba.ddsi.clienteliviano.models.vista.EntregaVista(
        dto.idEntrega(),
        dto.donacionId(),
        descripcionDonacion != null ? descripcionDonacion : "Donación #" + dto.donacionId(),
        dto.patenteCamion(),
        null,
        dto.estadoActual(),
        lat,
        lon,
        dto.ultimaActualizacion(),
        null
    );
  }

  /**
   * Confirma la recepción de una donación enviando la evidencia fotográfica.
   * Consume POST {backend.api.url.donaciones}/donaciones-independientes/{id}/recepcion
   */
  public ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion confirmarRecepcion(
      Long donacionId, java.util.List<String> urls, String observaciones, boolean completa) {
    if (donacionId == null) {
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("ID de donación inválido.");
    }
    String url = backendApiUrl + "/donaciones-independientes/" + donacionId + "/recepcion";
    var request = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.RecepcionRequest(urls, observaciones, completa);

    try {
      var response = restTemplate.postForEntity(url, request, Void.class);
      if (response.getStatusCode().is2xxSuccessful()) {
        return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.ok("Confirmación de recepción registrada.");
      }
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("No se pudo registrar la confirmación (código: " + response.getStatusCode().value() + ").");
    } catch (org.springframework.web.client.HttpStatusCodeException e) {
      log.warn("Error HTTP al confirmar recepción de donación {}: {} - {}", donacionId, e.getStatusCode(), e.getResponseBodyAsString());
      int status = e.getStatusCode().value();
      if (status == 403) {
        return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("Esa donación no es tuya o no tenés permiso para confirmarla.");
      } else if (status == 400) {
        return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("Datos de confirmación inválidos: " + e.getStatusText());
      } else if (status == 404) {
        return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("La donación no fue encontrada.");
      } else if (status == 409) {
        return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("La donación o su entrega ya fue resuelta o no está en condiciones de confirmarse.");
      } else {
        return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("No pudimos registrar la recepción en el servidor, por favor reintentá.");
      }
    } catch (Exception e) {
      log.error("Fallo de conexión al confirmar recepción de donación {}: {}", donacionId, e.getMessage());
      return ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion.error("No pudimos registrar la recepción en el servidor, por favor reintentá.");
    }
  }
}
