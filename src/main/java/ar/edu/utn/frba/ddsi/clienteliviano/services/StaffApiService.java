package ar.edu.utn.frba.ddsi.clienteliviano.services;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.BienCreateRequest;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.CamionCreateRequest;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.CamionForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.CamionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.CamionUpdateRequest;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionCreateRequest;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionIndependienteResumenResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonanteAltaForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonanteResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.PageResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.SubcategoriaResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.AsignacionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ConfirmarAsignacionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionDetalleResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.RankingMensualResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.RankingFila;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ImportacionInicioResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ImportacionJobResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoImportacion;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Servicio cliente para el área de administración (staff).
 * Centraliza la comunicación con los microservicios backend y traduce
 * errores HTTP en mensajes amigables para el usuario (ResultadoOperacion).
 */
@Service
public class StaffApiService {

  private static final Logger log = LoggerFactory.getLogger(StaffApiService.class);

  private final RestTemplate restTemplate;

  @Value("${backend.api.url.donaciones:http://localhost:8080/donaciones-service}")
  private String donacionesUrl;

  @Value("${backend.api.url.logistica:http://localhost:8083}")
  private String logisticaUrl;

  @Value("${backend.api.url.incentivos:http://localhost:8082/incentivos-service}")
  private String incentivosUrl;

  public StaffApiService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /* =========================================================
     Módulo Camiones (logistica-service)
     ========================================================= */

  public List<CamionResponse> camiones() {
    try {
      var respuesta = restTemplate.getForEntity(logisticaUrl + "/camiones", CamionResponse[].class);
      if (respuesta.getStatusCode().is2xxSuccessful() && respuesta.getBody() != null) {
        return Arrays.asList(respuesta.getBody());
      }
      return List.of();
    } catch (Exception e) {
      log.warn("Error al listar camiones desde {}: {}", logisticaUrl, e.getMessage());
      return List.of();
    }
  }

  public Optional<CamionResponse> camionPorId(String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }
    try {
      var respuesta = restTemplate.getForEntity(logisticaUrl + "/camiones/" + id, CamionResponse.class);
      return respuesta.getStatusCode().is2xxSuccessful()
          ? Optional.ofNullable(respuesta.getBody())
          : Optional.empty();
    } catch (Exception e) {
      log.warn("Error al obtener camión con id {} desde {}: {}", id, logisticaUrl, e.getMessage());
      return Optional.empty();
    }
  }

  public ResultadoOperacion crearCamion(CamionForm form) {
    if (form == null) {
      return ResultadoOperacion.error("Formulario de camión no provisto.");
    }
    if (form.getPatente() == null || form.getPatente().trim().isBlank()) {
      return ResultadoOperacion.error("La patente es obligatoria.");
    }
    if (form.getCapacidadDeCarga() == null || form.getCapacidadDeCarga() <= 0) {
      return ResultadoOperacion.error("La capacidad de carga debe ser mayor a 0 kg.");
    }
    if (form.getVolumen() == null || form.getVolumen() <= 0) {
      return ResultadoOperacion.error("El volumen debe ser mayor a 0 m³.");
    }
    if (form.getAltura() == null || form.getAltura() <= 0) {
      return ResultadoOperacion.error("La altura debe ser mayor a 0 m.");
    }

    var request = new CamionCreateRequest(
        form.getPatente().trim().toUpperCase(),
        form.getVolumen(),
        form.getAltura(),
        form.getCapacidadDeCarga(),
        form.getLatitud() != null ? form.getLatitud() : 0.0,
        form.getLongitud() != null ? form.getLongitud() : 0.0
    );

    try {
      var respuesta = restTemplate.postForEntity(logisticaUrl + "/camiones", request, CamionResponse.class);
      if (respuesta.getStatusCode().is2xxSuccessful()) {
        return ResultadoOperacion.ok("Agregamos el camión " + form.getPatente().trim().toUpperCase() + " a la flota.");
      }
      return ResultadoOperacion.error("No se pudo registrar el camión en la flota.");
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al crear camión: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      return ResultadoOperacion.error(extraerMensajeError(e, "Error al registrar el camión."));
    } catch (Exception e) {
      log.error("Error inesperado al crear camión: {}", e.getMessage());
      return ResultadoOperacion.error("No se pudo conectar con el servicio de logística.");
    }
  }

  public ResultadoOperacion actualizarCamion(String id, CamionForm form) {
    if (id == null || id.isBlank()) {
      return ResultadoOperacion.error("Identificador de camión inválido.");
    }
    if (form == null) {
      return ResultadoOperacion.error("Formulario de camión no provisto.");
    }
    if (form.getPatente() == null || form.getPatente().trim().isBlank()) {
      return ResultadoOperacion.error("La patente es obligatoria.");
    }
    if (form.getCapacidadDeCarga() == null || form.getCapacidadDeCarga() <= 0) {
      return ResultadoOperacion.error("La capacidad de carga debe ser mayor a 0 kg.");
    }
    if (form.getVolumen() == null || form.getVolumen() <= 0) {
      return ResultadoOperacion.error("El volumen debe ser mayor a 0 m³.");
    }
    if (form.getAltura() == null || form.getAltura() <= 0) {
      return ResultadoOperacion.error("La altura debe ser mayor a 0 m.");
    }

    var request = new CamionUpdateRequest(
        form.getPatente().trim().toUpperCase(),
        form.getVolumen(),
        form.getAltura(),
        form.getCapacidadDeCarga()
    );

    try {
      var respuesta = restTemplate.exchange(
          logisticaUrl + "/camiones/" + id,
          HttpMethod.PUT,
          new HttpEntity<>(request),
          CamionResponse.class
      );
      if (respuesta.getStatusCode().is2xxSuccessful()) {
        return ResultadoOperacion.ok("Actualizamos el camión " + form.getPatente().trim().toUpperCase() + " correctamente.");
      }
      return ResultadoOperacion.error("No se pudo actualizar el camión.");
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al actualizar camión {}: {} - {}", id, e.getStatusCode(), e.getResponseBodyAsString());
      return ResultadoOperacion.error(extraerMensajeError(e, "Error al actualizar el camión."));
    } catch (Exception e) {
      log.error("Error inesperado al actualizar camión {}: {}", id, e.getMessage());
      return ResultadoOperacion.error("No se pudo conectar con el servicio de logística.");
    }
  }

  public ResultadoOperacion eliminarCamion(String id) {
    if (id == null || id.isBlank()) {
      return ResultadoOperacion.error("Identificador de camión inválido.");
    }
    try {
      restTemplate.delete(logisticaUrl + "/camiones/" + id);
      return ResultadoOperacion.ok("Dimos de baja el camión correctamente.");
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al eliminar camión {}: {} - {}", id, e.getStatusCode(), e.getResponseBodyAsString());
      return ResultadoOperacion.error(extraerMensajeError(e, "No se pudo eliminar el camión."));
    } catch (Exception e) {
      log.error("Error inesperado al eliminar camión {}: {}", id, e.getMessage());
      return ResultadoOperacion.error("No se pudo conectar con el servicio de logística.");
    }
  }

  /* =========================================================
     Módulo Donantes (donaciones-service)
     ========================================================= */

  public PageResponse<DonanteResponse> buscarDonantes(int page, int size, String q) {
    try {
      var uriBuilder = UriComponentsBuilder.fromUriString(donacionesUrl + "/donantes")
          .queryParam("page", page)
          .queryParam("size", size);
      if (q != null && !q.trim().isBlank()) {
        uriBuilder.queryParam("q", q.trim());
      }
      var uri = uriBuilder.toUriString();

      var respuesta = restTemplate.exchange(
          uri,
          HttpMethod.GET,
          null,
          new ParameterizedTypeReference<PageResponse<DonanteResponse>>() {}
      );

      if (respuesta.getStatusCode().is2xxSuccessful() && respuesta.getBody() != null) {
        return respuesta.getBody();
      }
      return new PageResponse<>(List.of(), page, size, 0, 0);
    } catch (Exception e) {
      log.warn("Error al buscar donantes desde {}: {}", donacionesUrl, e.getMessage());
      return new PageResponse<>(List.of(), page, size, 0, 0);
    }
  }

  public ResultadoOperacion crearDonante(DonanteAltaForm form) {
    if (form == null) {
      return ResultadoOperacion.error("Formulario de donante no provisto.");
    }
    if (Boolean.TRUE.equals(form.getEsJuridico())) {
      if (form.getRazonSocial() == null || form.getRazonSocial().trim().isBlank()) {
        return ResultadoOperacion.error("La razón social es obligatoria para organizaciones.");
      }
    } else {
      if (form.getNombre() == null || form.getNombre().trim().isBlank()) {
        return ResultadoOperacion.error("El nombre es obligatorio.");
      }
      if (form.getApellido() == null || form.getApellido().trim().isBlank()) {
        return ResultadoOperacion.error("El apellido es obligatorio.");
      }
    }
    if (form.getDocumento() == null || form.getDocumento().trim().isBlank()) {
      return ResultadoOperacion.error("El número de documento es obligatorio para identificar al donante.");
    }

    try {
      var respuesta = restTemplate.postForEntity(donacionesUrl + "/donantes", form, DonanteResponse.class);
      if (respuesta.getStatusCode().is2xxSuccessful()) {
        String mensaje = Boolean.TRUE.equals(form.getCrearUsuario())
            ? "Dimos de alta al donante y solicitamos la creación de su cuenta de usuario."
            : "Dimos de alta al donante correctamente.";
        return ResultadoOperacion.ok(mensaje);
      }
      return ResultadoOperacion.error("No se pudo dar de alta al donante.");
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al dar de alta donante: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      return ResultadoOperacion.error(extraerMensajeError(e, "Error al registrar el donante."));
    } catch (Exception e) {
      log.error("Error inesperado al crear donante: {}", e.getMessage());
      return ResultadoOperacion.error("No se pudo conectar con el servicio de donaciones.");
    }
  }

  public ResultadoImportacion iniciarImportacionCsv(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      return ResultadoImportacion.error("Elegí un archivo CSV antes de importar.");
    }
    String nombre = archivo.getOriginalFilename();
    if (nombre == null || !nombre.toLowerCase().endsWith(".csv")) {
      return ResultadoImportacion.error("El archivo debe tener extensión .csv");
    }

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("archivo", new ByteArrayResource(archivo.getBytes()) {
        @Override
        public String getFilename() {
          return archivo.getOriginalFilename();
        }
      });

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
      ResponseEntity<ImportacionInicioResponse> response = restTemplate.postForEntity(
          donacionesUrl + "/donantes/importar",
          requestEntity,
          ImportacionInicioResponse.class
      );

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        return ResultadoImportacion.ok("Importación iniciada con éxito", response.getBody().jobId());
      }
      return ResultadoImportacion.error("No se pudo iniciar el proceso de importación.");
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al importar CSV de donantes: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      if (e.getStatusCode() == HttpStatus.PAYLOAD_TOO_LARGE) {
        return ResultadoImportacion.error("El archivo supera el tamaño máximo permitido.");
      }
      if (e.getStatusCode() == HttpStatus.BAD_GATEWAY || e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
        return ResultadoImportacion.error("El servicio de donaciones no se encuentra disponible temporalmente.");
      }
      return ResultadoImportacion.error(extraerMensajeError(e, "Error al procesar el archivo CSV."));
    } catch (Exception e) {
      log.error("Error inesperado al importar CSV de donantes: {}", e.getMessage(), e);
      return ResultadoImportacion.error("Error inesperado al conectar con el servidor.");
    }
  }

  public Optional<ImportacionJobResponse> consultarEstadoImportacion(String jobId) {
    if (jobId == null || jobId.isBlank()) {
      return Optional.empty();
    }
    try {
      ResponseEntity<ImportacionJobResponse> response = restTemplate.getForEntity(
          donacionesUrl + "/donantes/importar/" + jobId,
          ImportacionJobResponse.class
      );
      return Optional.ofNullable(response.getBody());
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al consultar estado de importación {}: {}", jobId, e.getStatusCode());
      return Optional.empty();
    } catch (Exception e) {
      log.error("Error al consultar estado de importación {}: {}", jobId, e.getMessage(), e);
      return Optional.empty();
    }
  }

  /* =========================================================
     Módulo Donaciones e Independientes (donaciones-service)
     ========================================================= */

  public List<SubcategoriaResponse> subcategorias() {
    try {
      var respuesta = restTemplate.exchange(
          donacionesUrl + "/subcategorias",
          HttpMethod.GET,
          null,
          new ParameterizedTypeReference<List<SubcategoriaResponse>>() {}
      );
      if (respuesta.getStatusCode().is2xxSuccessful() && respuesta.getBody() != null) {
        return respuesta.getBody();
      }
      return List.of();
    } catch (Exception e) {
      log.warn("Error al listar subcategorías desde {}: {}", donacionesUrl, e.getMessage());
      return List.of();
    }
  }

  public ResultadoOperacion crearDonacion(DonacionForm form) {
    if (form == null) {
      return ResultadoOperacion.error("Formulario de donación no provisto.");
    }
    if (form.getDonanteId() == null) {
      return ResultadoOperacion.error("Elegí a quién pertenece la donación antes de registrarla.");
    }
    if (form.getBienes() == null || form.getBienes().isEmpty()) {
      return ResultadoOperacion.error("Debes incluir al menos un bien en el lote.");
    }

    List<BienCreateRequest> bienesPayload = new ArrayList<>();
    for (int i = 0; i < form.getBienes().size(); i++) {
      var b = form.getBienes().get(i);
      if (b.getSubcategoriaId() == null) {
        return ResultadoOperacion.error("Seleccioná la subcategoría para cada bien del lote (bien #" + (i + 1) + ").");
      }
      if (b.getCantidad() == null || b.getCantidad() <= 0) {
        return ResultadoOperacion.error("La cantidad de cada bien debe ser mayor a 0 (bien #" + (i + 1) + ").");
      }
      double peso = b.getPeso() != null && b.getPeso() >= 0 ? b.getPeso() : 0.0;
      double volumen = b.getVolumen() != null && b.getVolumen() >= 0 ? b.getVolumen() : 0.0;
      String desc = b.getDescripcion() != null && !b.getDescripcion().isBlank() ? b.getDescripcion().trim() : "Bien";

      bienesPayload.add(new BienCreateRequest(
          desc,
          b.getFoto(),
          b.getSubcategoriaId(),
          b.getCantidad(),
          b.isEsUsado(),
          b.getFechaDeVencimiento(),
          peso,
          volumen
      ));
    }

    String descripcion = form.getDescripcion() != null && !form.getDescripcion().isBlank()
        ? form.getDescripcion().trim()
        : "Lote de donación";

    var request = new DonacionCreateRequest(descripcion, form.getDonanteId(), bienesPayload);

    try {
      var respuesta = restTemplate.postForEntity(donacionesUrl + "/donaciones", request, Void.class);
      if (respuesta.getStatusCode().is2xxSuccessful()) {
        return ResultadoOperacion.ok("Registramos la donación. Ya entró al circuito de asignación.");
      }
      return ResultadoOperacion.error("No se pudo registrar la donación.");
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al registrar donación: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      return ResultadoOperacion.error(extraerMensajeError(e, "Error al registrar la donación."));
    } catch (Exception e) {
      log.error("Error inesperado al registrar donación: {}", e.getMessage());
      return ResultadoOperacion.error("No se pudo conectar con el servicio de donaciones.");
    }
  }

  public PageResponse<DonacionIndependienteResumenResponse> donacionesIndependientes(String estado, int page, int size) {
    try {
      var uriBuilder = UriComponentsBuilder.fromUriString(donacionesUrl + "/donaciones-independientes")
          .queryParam("page", page)
          .queryParam("size", size);
      if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
        uriBuilder.queryParam("estado", estado.trim().toUpperCase());
      }
      var uri = uriBuilder.toUriString();

      var respuesta = restTemplate.exchange(
          uri,
          HttpMethod.GET,
          null,
          new ParameterizedTypeReference<PageResponse<DonacionIndependienteResumenResponse>>() {}
      );

      if (respuesta.getStatusCode().is2xxSuccessful() && respuesta.getBody() != null) {
        return respuesta.getBody();
      }
      return new PageResponse<>(List.of(), page, size, 0, 0);
    } catch (Exception e) {
      log.warn("Error al listar donaciones independientes desde {}: {}", donacionesUrl, e.getMessage());
      return new PageResponse<>(List.of(), page, size, 0, 0);
    }
  }

  public ResultadoOperacion marcarDonacionVencida(Long id) {
    if (id == null) {
      return ResultadoOperacion.error("Identificador de donación no provisto.");
    }
    try {
      String url = donacionesUrl + "/donaciones-independientes/" + id + "/estado?nuevoEstado=VENCIDA&descripcion=Vencimiento";
      restTemplate.postForEntity(url, null, Void.class);
      return ResultadoOperacion.ok("Marcamos la donación #" + id + " como vencida. Sale del circuito de asignación.");
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al marcar donación {} como vencida: {} - {}", id, e.getStatusCode(), e.getResponseBodyAsString());
      return ResultadoOperacion.error(extraerMensajeError(e, "No se pudo marcar la donación como vencida."));
    } catch (Exception e) {
      log.error("Error inesperado al marcar donación {} como vencida: {}", id, e.getMessage());
      return ResultadoOperacion.error("No se pudo conectar con el servicio de donaciones.");
    }
  }

  public Optional<DonacionVista> obtenerDonacionDetalle(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    try {
      var resp = restTemplate.getForObject(donacionesUrl + "/donaciones-independientes/" + id, DonacionDetalleResponse.class);
      if (resp != null) {
        boolean vencida = resp.vencimiento() != null && resp.vencimiento().isBefore(LocalDate.now());
        DonacionVista vista = new DonacionVista(
            resp.id(),
            resp.descripcion(),
            resp.estado(),
            resp.fecha(),
            resp.vencimiento(),
            vencida,
            resp.donanteId(),
            resp.donanteNombre(),
            resp.entidadId(),
            resp.entidadNombre(),
            resp.cantidadBienes(),
            null,
            List.of()
        );
        return Optional.of(vista);
      }
      return Optional.empty();
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al obtener donación independiente {}: {}", id, e.getStatusCode());
      return Optional.empty();
    } catch (Exception e) {
      log.error("Error inesperado al obtener donación independiente {}: {}", id, e.getMessage());
      return Optional.empty();
    }
  }

  public Optional<AsignacionResponse> obtenerPropuestas(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    try {
      var resp = restTemplate.getForObject(donacionesUrl + "/" + id + "/propuestas", AsignacionResponse.class);
      return Optional.ofNullable(resp);
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al obtener propuestas para donación {}: {}", id, e.getStatusCode());
      return Optional.empty();
    } catch (Exception e) {
      log.error("Error inesperado al obtener propuestas para donación {}: {}", id, e.getMessage());
      return Optional.empty();
    }
  }

  public ResultadoOperacion ejecutarAlgoritmos(Long id) {
    if (id == null) {
      return ResultadoOperacion.error("Identificador de donación no provisto.");
    }
    try {
      var resp = restTemplate.postForEntity(donacionesUrl + "/" + id + "/ejecutar-algoritmos", null, AsignacionResponse.class);
      if (resp.getStatusCode().is2xxSuccessful()) {
        return ResultadoOperacion.ok("Algoritmos ejecutados con éxito. Mostramos las nuevas propuestas calculadas.");
      }
      return ResultadoOperacion.error("No se pudieron ejecutar los algoritmos de asignación.");
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al ejecutar algoritmos para donación {}: {} - {}", id, e.getStatusCode(), e.getResponseBodyAsString());
      return ResultadoOperacion.error(extraerMensajeError(e, "Error al ejecutar los algoritmos de asignación."));
    } catch (Exception e) {
      log.error("Error inesperado al ejecutar algoritmos para donación {}: {}", id, e.getMessage());
      return ResultadoOperacion.error("No se pudo conectar con el servicio de donaciones.");
    }
  }

  public ResultadoOperacion confirmarAsignacion(Long donacionId, Long necesidadId) {
    if (donacionId == null || necesidadId == null) {
      return ResultadoOperacion.error("Debe seleccionar una necesidad para confirmar la asignación.");
    }
    try {
      String url = donacionesUrl + "/" + donacionId + "/confirmar-asignacion?necesidadId=" + necesidadId;
      var resp = restTemplate.postForEntity(url, null, ConfirmarAsignacionResponse.class);
      if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
        var body = resp.getBody();
        if (body.advertencia() != null && !body.advertencia().isBlank()) {
          return ResultadoOperacion.ok("Asignamos la donación a la entidad, pero con aviso de logística: " + body.advertencia());
        }
        String entidad = body.entidadNombre() != null ? body.entidadNombre() : "la entidad";
        return ResultadoOperacion.ok("Asignamos la donación #" + donacionId + " a " + entidad + " con éxito.");
      }
      return ResultadoOperacion.ok("Asignamos la donación #" + donacionId + " con éxito.");
    } catch (HttpStatusCodeException e) {
      log.warn("Error HTTP al confirmar asignación para donación {}: {} - {}", donacionId, e.getStatusCode(), e.getResponseBodyAsString());
      return ResultadoOperacion.error(extraerMensajeError(e, "No se pudo confirmar la asignación."));
    } catch (Exception e) {
      log.error("Error inesperado al confirmar asignación para donación {}: {}", donacionId, e.getMessage());
      return ResultadoOperacion.error("No se pudo conectar con el servicio de donaciones.");
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

  /* =========================================================
     Manejo de errores amigables
     ========================================================= */

  private String extraerMensajeError(HttpStatusCodeException e, String mensajePorDefecto) {
    try {
      String body = e.getResponseBodyAsString();
      if (body != null && !body.isBlank()) {
        try {
          com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
          if (root.hasNonNull("message") && !root.get("message").asText().isBlank()) {
            return root.get("message").asText();
          }
          if (root.hasNonNull("mensaje") && !root.get("mensaje").asText().isBlank()) {
            return root.get("mensaje").asText();
          }
          if (root.hasNonNull("error")) {
            String err = root.get("error").asText();
            if (!err.isBlank() && !"Conflict".equalsIgnoreCase(err) && !"Internal Server Error".equalsIgnoreCase(err)) {
              return err;
            }
          }
        } catch (Exception parseEx) {
          log.debug("No se pudo parsear el cuerpo del error como JSON: {}", parseEx.getMessage());
        }
      }
    } catch (Exception ignored) {
    }

    if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
      return "No tienes permisos de administrador para realizar esta acción.";
    }
    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
      return "El recurso solicitado no fue encontrado.";
    }
    if (e.getStatusCode() == HttpStatus.CONFLICT) {
      return "Conflicto con los datos existentes o estado de la donación inválido.";
    }
    if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
      return "Los datos provistos son inválidos.";
    }
    if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
      return "Error al comunicarse con el servidor de identidad Keycloak.";
    }
    return mensajePorDefecto;
  }
}
