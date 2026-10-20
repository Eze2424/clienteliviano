package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.Toast;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.AsignacionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.CamionForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.CamionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.CategoriaResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionIndependienteResumenResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonanteAltaForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonanteResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ImportacionJobResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.PageResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion;
import ar.edu.utn.frba.ddsi.clienteliviano.models.entities.TipoOrganizacion;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.RankingFila;
import ar.edu.utn.frba.ddsi.clienteliviano.services.StaffApiService;
import ar.edu.utn.frba.ddsi.clienteliviano.web.Sesion;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
public class StaffController {

  private final Sesion sesion;
  private final RestTemplate restTemplate;
  private final StaffApiService staffApiService;

  @Value("${backend.api.url.donaciones}")
  private String donacionesUrl;

  @Value("${backend.api.url.logistica}")
  private String logisticaUrl;

  public StaffController(Sesion sesion, RestTemplate restTemplate, StaffApiService staffApiService) {
    this.sesion = sesion;
    this.restTemplate = restTemplate;
    this.staffApiService = staffApiService;
  }

  /**
   * Guard centralizado de acceso para el área de administración.
   * Retorna el usuario logueado si tiene rol ADMIN; null en caso contrario.
   */
  private UsuarioActual exigirAdmin(HttpSession session) {
    return sesion.tieneRol(session, Rol.ADMIN) ? sesion.usuario(session) : null;
  }

  /* ---------- CU2 y CU3: depósito ---------- */

  @GetMapping("/dashboard")
  public String dashboard(
      @RequestParam(required = false) String estado,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpSession session,
      Model model) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    // 1. Donaciones paginadas filtradas para la tabla
    PageResponse<DonacionIndependienteResumenResponse> paginaResumen =
        staffApiService.donacionesIndependientes(estado, page, size);
    if (paginaResumen == null) {
      paginaResumen = new PageResponse<>(List.of(), page, size, 0, 0);
    }

    List<DonacionVista> donaciones = paginaResumen.content().stream()
        .map(DonacionVista::desdeResumen)
        .toList();

    // 2. Métricas consolidadas para las cards (muestreo completo)
    PageResponse<DonacionIndependienteResumenResponse> todas =
        staffApiService.donacionesIndependientes(null, 0, 1000);
    if (todas == null) {
      todas = new PageResponse<>(List.of(), 0, 1000, 0, 0);
    }

    long totalRegistradas = todas.totalElements();
    long sinAsignar = todas.content().stream()
        .filter(d -> "EN_DEPOSITO".equalsIgnoreCase(d.estado()))
        .count();
    long vencidasTotal = todas.content().stream()
        .filter(d -> d.vencida() || "VENCIDA".equalsIgnoreCase(d.estado()))
        .count();

    List<CamionResponse> camiones = staffApiService.camiones();

    model.addAttribute("donaciones", donaciones);
    model.addAttribute("pagina", paginaResumen);
    model.addAttribute("estadoSeleccionado", estado != null && !estado.isBlank() ? estado.toUpperCase() : "TODOS");
    model.addAttribute("totalRegistradas", totalRegistradas);
    model.addAttribute("sinAsignar", sinAsignar);
    model.addAttribute("vencidasTotal", vencidasTotal);
    model.addAttribute("camiones", camiones != null ? camiones : List.of());

    return "staff/dashboard";
  }

  @PostMapping("/donaciones/{id}/vencida")
  public String marcarVencida(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    ResultadoOperacion resultado = staffApiService.marcarDonacionVencida(id);
    if (resultado.exito()) {
      redirect.addFlashAttribute("toast", Toast.exito(resultado.mensaje()));
    } else {
      redirect.addFlashAttribute("toast", Toast.error(resultado.mensaje()));
    }

    return "redirect:/staff/dashboard";
  }

  @GetMapping("/donaciones/{id}/asignar")
  public String asignar(@PathVariable Long id, HttpSession session, Model model) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    var donacionOpt = staffApiService.obtenerDonacionDetalle(id);
    if (donacionOpt.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Donación inexistente");
    }

    var propuestasOpt = staffApiService.obtenerPropuestas(id);
    AsignacionResponse propuestas = propuestasOpt.orElseGet(() ->
        new AsignacionResponse(id, List.of(), List.of(), List.of(), false)
    );

    model.addAttribute("donacion", donacionOpt.get());
    model.addAttribute("propuestas", propuestas);

    return "staff/asignar";
  }

  @PostMapping("/donaciones/{id}/ejecutar-algoritmos")
  public String ejecutarAlgoritmos(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    ResultadoOperacion resultado = staffApiService.ejecutarAlgoritmos(id);
    if (resultado.exito()) {
      redirect.addFlashAttribute("toast", Toast.exito(resultado.mensaje()));
    } else {
      redirect.addFlashAttribute("toast", Toast.error(resultado.mensaje()));
    }

    return "redirect:/staff/donaciones/" + id + "/asignar";
  }

  @PostMapping("/donaciones/{id}/asignar")
  public String confirmarAsignacion(@PathVariable Long id,
                                    @RequestParam Long necesidadId,
                                    HttpSession session,
                                    RedirectAttributes redirect) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    ResultadoOperacion resultado = staffApiService.confirmarAsignacion(id, necesidadId);
    if (resultado.exito()) {
      redirect.addFlashAttribute("toast", Toast.exito(resultado.mensaje()));
      return "redirect:/staff/dashboard";
    } else {
      redirect.addFlashAttribute("toast", Toast.error(resultado.mensaje()));
      return "redirect:/staff/donaciones/" + id + "/asignar";
    }
  }

  /* ---------- CU1: alta de donación ---------- */

  @GetMapping("/donaciones/nueva")
  public String nuevaDonacion(HttpSession session, Model model) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    cargarDatosNuevaDonacion(model);
    if (!model.containsAttribute("form")) {
      model.addAttribute("form", new DonacionForm());
    }
    return "staff/nueva-donacion";
  }

  @PostMapping("/donaciones/nueva")
  public String registrarDonacion(@ModelAttribute("form") DonacionForm form,
                                  HttpSession session, Model model,
                                  RedirectAttributes redirect) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    ResultadoOperacion resultado = staffApiService.crearDonacion(form);
    if (resultado.exito()) {
      redirect.addFlashAttribute("toast", Toast.exito(resultado.mensaje()));
      return "redirect:/staff/dashboard";
    }

    model.addAttribute("error", resultado.mensaje());
    cargarDatosNuevaDonacion(model);
    return "staff/nueva-donacion";
  }

  private void cargarDatosNuevaDonacion(Model model) {
    List<CategoriaResponse> categorias = staffApiService.categorias();
    model.addAttribute("categorias", categorias != null ? categorias : List.of());

    PageResponse<DonanteResponse> primeraPaginaDonantes = staffApiService.buscarDonantes(0, 20, null);
    model.addAttribute("donantes", primeraPaginaDonantes != null ? primeraPaginaDonantes.content() : List.of());
  }

  @GetMapping("/api/donantes")
  @ResponseBody
  public PageResponse<DonanteResponse> buscarDonantesApi(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String q,
      HttpSession session) {
    if (exigirAdmin(session) == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autorizado");
    }
    return staffApiService.buscarDonantes(page, size, q);
  }

  /* ---------- CU1 y CU6: donantes e importación ---------- */

  @GetMapping("/donantes")
  public String donantes(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String q,
      HttpSession session,
      Model model) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    PageResponse<DonanteResponse> pagina = staffApiService.buscarDonantes(page, size, q);
    model.addAttribute("pagina", pagina);
    model.addAttribute("donantes", pagina.content());
    model.addAttribute("q", q != null ? q : "");
    model.addAttribute("tiposOrganizacion", TipoOrganizacion.values());

    if (!model.containsAttribute("form")) {
      model.addAttribute("form", new DonanteAltaForm());
    }
    return "staff/donantes";
  }

  @PostMapping("/donantes")
  public String altaDonante(@ModelAttribute("form") DonanteAltaForm form,
                            HttpSession session,
                            RedirectAttributes redirect) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    ResultadoOperacion resultado = staffApiService.crearDonante(form);
    if (resultado.exito()) {
      redirect.addFlashAttribute("toast", Toast.exito(resultado.mensaje()));
    } else {
      redirect.addFlashAttribute("toast", Toast.error(resultado.mensaje()));
      redirect.addFlashAttribute("form", form);
    }

    return "redirect:/staff/donantes";
  }

  @PostMapping("/donantes/importar")
  public String importarCsv(@RequestParam("archivo") MultipartFile archivo,
                            HttpSession session, RedirectAttributes redirect) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    if (archivo == null || archivo.isEmpty()) {
      redirect.addFlashAttribute("toast", Toast.error("Elegí un archivo CSV antes de importar."));
      return "redirect:/staff/donantes";
    }

    String nombre = archivo.getOriginalFilename();
    if (nombre == null || !nombre.toLowerCase().endsWith(".csv")) {
      redirect.addFlashAttribute("toast", Toast.error("El archivo debe ser un CSV con extensión .csv"));
      return "redirect:/staff/donantes";
    }

    if (archivo.getSize() > 10 * 1024 * 1024) {
      redirect.addFlashAttribute("toast", Toast.error("El archivo supera el tamaño máximo permitido de 10 MB."));
      return "redirect:/staff/donantes";
    }

    var resultado = staffApiService.iniciarImportacionCsv(archivo);
    if (resultado.exito()) {
      return "redirect:/staff/donantes/importar?jobId=" + resultado.jobId();
    } else {
      redirect.addFlashAttribute("toast", Toast.error(resultado.mensaje()));
      return "redirect:/staff/donantes";
    }
  }

  @GetMapping("/donantes/importar")
  public String verProgresoImportacion(@RequestParam String jobId, HttpSession session, Model model) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    var jobOpt = staffApiService.consultarEstadoImportacion(jobId);
    ImportacionJobResponse job = jobOpt.orElseGet(() ->
        new ImportacionJobResponse(jobId, "EN_CURSO", 0, 0, 0, 0, List.of(), "Iniciando proceso...")
    );

    model.addAttribute("job", job);
    return "staff/importar";
  }

  @GetMapping("/api/donantes/importar/{jobId}")
  @ResponseBody
  public ImportacionJobResponse consultarEstadoApi(@PathVariable String jobId, HttpSession session) {
    if (exigirAdmin(session) == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autorizado");
    }
    return staffApiService.consultarEstadoImportacion(jobId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job no encontrado"));
  }

  /* ---------- CU4: camiones ---------- */

  @GetMapping("/camiones")
  public String camiones(HttpSession session, Model model,
                         @RequestParam(required = false) String editarId) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    List<CamionResponse> camiones = staffApiService.camiones();
    model.addAttribute("camiones", camiones);

    if (editarId != null && !editarId.isBlank()) {
      var camionOpt = staffApiService.camionPorId(editarId);
      if (camionOpt.isPresent()) {
        model.addAttribute("form", CamionForm.desde(camionOpt.get()));
        model.addAttribute("editando", true);
      } else {
        model.addAttribute("form", new CamionForm());
        model.addAttribute("editando", false);
      }
    } else if (!model.containsAttribute("form")) {
      model.addAttribute("form", new CamionForm());
      model.addAttribute("editando", false);
    }
    return "staff/camiones";
  }

  @PostMapping("/camiones")
  public String altaCamion(@ModelAttribute("form") CamionForm form,
                           HttpSession session,
                           RedirectAttributes redirect) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    ResultadoOperacion resultado = staffApiService.crearCamion(form);
    if (resultado.exito()) {
      redirect.addFlashAttribute("toast", Toast.exito(resultado.mensaje()));
    } else {
      redirect.addFlashAttribute("toast", Toast.error(resultado.mensaje()));
    }

    return "redirect:/staff/camiones";
  }

  @PostMapping("/camiones/{id}")
  public String editarCamion(@PathVariable String id,
                             @ModelAttribute("form") CamionForm form,
                             HttpSession session,
                             RedirectAttributes redirect) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    ResultadoOperacion resultado = staffApiService.actualizarCamion(id, form);
    if (resultado.exito()) {
      redirect.addFlashAttribute("toast", Toast.exito(resultado.mensaje()));
    } else {
      redirect.addFlashAttribute("toast", Toast.error(resultado.mensaje()));
    }

    return "redirect:/staff/camiones";
  }

  @PostMapping("/camiones/{id}/eliminar")
  public String bajaCamion(@PathVariable String id, HttpSession session,
                           RedirectAttributes redirect) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    ResultadoOperacion resultado = staffApiService.eliminarCamion(id);
    if (resultado.exito()) {
      redirect.addFlashAttribute("toast", Toast.exito(resultado.mensaje()));
    } else {
      redirect.addFlashAttribute("toast", Toast.error(resultado.mensaje()));
    }

    return "redirect:/staff/camiones";
  }

  /* ---------- CU5: ranking ---------- */

  @GetMapping("/ranking")
  public String ranking(HttpSession session, Model model,
                        @RequestParam(required = false) String periodo) {
    if (exigirAdmin(session) == null) return "redirect:/login";

    List<String> periodosDeRanking = staffApiService.obtenerPeriodosRanking();
    String periodoElegido = (periodo != null && !periodo.isBlank())
        ? periodo.trim()
        : (periodosDeRanking != null && !periodosDeRanking.isEmpty()
            ? periodosDeRanking.get(0)
            : java.time.YearMonth.now().toString());

    List<RankingFila> ranking = staffApiService.obtenerRanking(periodoElegido, 10);

    model.addAttribute("ranking", ranking != null ? ranking : List.of());
    model.addAttribute("periodos", periodosDeRanking != null ? periodosDeRanking : List.of());
    model.addAttribute("periodoActual", periodoElegido);

    return "staff/ranking";
  }
}
