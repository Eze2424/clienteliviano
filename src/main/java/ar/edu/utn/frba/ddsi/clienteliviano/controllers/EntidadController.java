package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.Toast;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ConfirmacionRecepcionForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadForm;
import ar.edu.utn.frba.ddsi.clienteliviano.web.Sesion;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Área de la entidad beneficiaria. Cubre sus cinco casos de uso:
 *   1. registrar necesidades materiales            -> /entidad/necesidades
 *   2. ver el estado de las donaciones asignadas   -> /entidad/dashboard
 *   3. confirmar recepción con fotos               -> /entidad/donaciones/{id}
 *   4. recibir notificaciones                      -> panel del dashboard
 *   5. seguir las entregas activas en un mapa      -> /entidad/entregas
 *
 * El controller solo arma el Model y elige la vista. Cuando exista el servicio
 * cliente, se reemplaza `datos` por él y estos métodos no cambian de forma.
 */
@Controller
@RequestMapping("/entidad")
public class EntidadController {

  private final Sesion sesion;
  private final ar.edu.utn.frba.ddsi.clienteliviano.services.EntidadApiService entidadApiService;
  private final ar.edu.utn.frba.ddsi.clienteliviano.services.NotificacionApiService notificacionApiService;
  private final ar.edu.utn.frba.ddsi.clienteliviano.services.FotoRecepcionStorage fotoRecepcionStorage;

  public EntidadController(Sesion sesion,
                           ar.edu.utn.frba.ddsi.clienteliviano.services.EntidadApiService entidadApiService,
                           ar.edu.utn.frba.ddsi.clienteliviano.services.NotificacionApiService notificacionApiService,
                           ar.edu.utn.frba.ddsi.clienteliviano.services.FotoRecepcionStorage fotoRecepcionStorage) {
    this.sesion = sesion;
    this.entidadApiService = entidadApiService;
    this.notificacionApiService = notificacionApiService;
    this.fotoRecepcionStorage = fotoRecepcionStorage;
  }

  /** Sin sesión o con el rol equivocado, no se entra. */
  private UsuarioActual exigirEntidad(HttpSession session) {
    return sesion.tieneRol(session, Rol.ENTIDAD) ? sesion.usuario(session) : null;
  }

  /* ---------- CU2 y CU4: donaciones asignadas y notificaciones ---------- */

  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model) {
    UsuarioActual yo = exigirEntidad(session);
    if (yo == null) {
      return "redirect:/login";
    }

    var optDash = entidadApiService.dashboard();
    if (optDash.isPresent()) {
      var dash = optDash.get();
      var donacionesVista = dash.donaciones().stream()
          .map(d -> entidadApiService.mapearDonacion(d, yo.id()))
          .toList();
      model.addAttribute("donaciones", donacionesVista);
      model.addAttribute("entregasActivas", dash.entregasActivas());
      model.addAttribute("necesidadesActivas", dash.necesidadesActivas());
    } else {
      model.addAttribute("donaciones", java.util.List.of());
      model.addAttribute("entregasActivas", 0);
      model.addAttribute("necesidadesActivas", 0);
      model.addAttribute("error", "No pudimos conectar con el servidor de donaciones. Por favor, reintentá en unos momentos.");
    }

    var notifsDto = notificacionApiService.listar(yo.email(), yo.id());
    var notificaciones = notifsDto.stream()
        .map(notificacionApiService::mapear)
        .toList();
    model.addAttribute("notificaciones", notificaciones);
    long sinLeer = notificaciones.stream().filter(n -> !n.leida()).count();
    model.addAttribute("sinLeer", sinLeer);
    return "entidad/dashboard";
  }

  @PostMapping("/notificaciones/{id}/leida")
  public String marcarNotificacionLeida(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
    UsuarioActual yo = exigirEntidad(session);
    if (yo == null) {
      return "redirect:/login";
    }
    boolean ok = notificacionApiService.marcarLeida(id);
    if (!ok) {
      redirect.addFlashAttribute("toast", Toast.error("No se pudo marcar la notificación como leída."));
    }
    return "redirect:/entidad/dashboard#notificaciones";
  }


  /* ---------- CU1: registrar necesidades materiales ---------- */

  @GetMapping("/necesidades")
  public String necesidades(HttpSession session, Model model) {
    UsuarioActual yo = exigirEntidad(session);
    if (yo == null) {
      return "redirect:/login";
    }
    model.addAttribute("necesidades", entidadApiService.necesidades());
    model.addAttribute("subcategorias", entidadApiService.subcategorias());
    if (!model.containsAttribute("form")) {
      model.addAttribute("form", new NecesidadForm());
    }
    return "entidad/necesidades";
  }

  @PostMapping("/necesidades")
  public String registrarNecesidad(@ModelAttribute("form") NecesidadForm form,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirect) {
    UsuarioActual yo = exigirEntidad(session);
    if (yo == null) {
      return "redirect:/login";
    }

    if (form.getDescripcion() == null || form.getDescripcion().isBlank()) {
      model.addAttribute("necesidades", entidadApiService.necesidades());
      model.addAttribute("subcategorias", entidadApiService.subcategorias());
      model.addAttribute("error", "Contanos qué necesitás: la descripción no puede quedar vacía.");
      return "entidad/necesidades";
    }
    if (form.getSubcategoriaId() == null) {
      model.addAttribute("necesidades", entidadApiService.necesidades());
      model.addAttribute("subcategorias", entidadApiService.subcategorias());
      model.addAttribute("error", "Debes seleccionar una subcategoría de la lista.");
      return "entidad/necesidades";
    }
    if (form.getCantidadSolicitada() == null || form.getCantidadSolicitada() <= 0) {
      model.addAttribute("necesidades", entidadApiService.necesidades());
      model.addAttribute("subcategorias", entidadApiService.subcategorias());
      model.addAttribute("error", "La cantidad solicitada debe ser mayor a cero.");
      return "entidad/necesidades";
    }

    var res = entidadApiService.registrarNecesidad(yo.id(), form);
    if (res.exito()) {
      redirect.addFlashAttribute("toast", Toast.exito(res.mensaje()));
      return "redirect:/entidad/necesidades";
    } else {
      model.addAttribute("necesidades", entidadApiService.necesidades());
      model.addAttribute("subcategorias", entidadApiService.subcategorias());
      model.addAttribute("error", res.mensaje());
      return "entidad/necesidades";
    }
  }

  @PostMapping("/necesidades/{id}/eliminar")
  public String eliminarNecesidad(@PathVariable Long id,
                                  HttpSession session,
                                  RedirectAttributes redirect) {
    UsuarioActual yo = exigirEntidad(session);
    if (yo == null) {
      return "redirect:/login";
    }
    var res = entidadApiService.eliminarNecesidad(yo.id(), id);
    if (res.exito()) {
      redirect.addFlashAttribute("toast", Toast.exito(res.mensaje()));
    } else {
      redirect.addFlashAttribute("toast", Toast.error(res.mensaje()));
    }
    return "redirect:/entidad/necesidades";
  }


  /* ---------- CU5: seguimiento de entregas en el mapa ---------- */

  @GetMapping("/entregas")
  public String entregas(HttpSession session, Model model) {
    UsuarioActual yo = exigirEntidad(session);
    if (yo == null) {
      return "redirect:/login";
    }

    var optDash = entidadApiService.dashboard();
    if (optDash.isEmpty()) {
      model.addAttribute("entregas", java.util.List.of());
      model.addAttribute("error", "No pudimos conectar con los servicios de logística. Por favor, reintentá más tarde.");
      return "entidad/entregas";
    }

    var dash = optDash.get();
    var activas = dash.donaciones().stream()
        .filter(d -> "EN_TRASLADO".equalsIgnoreCase(d.estado()) || "LISTA".equalsIgnoreCase(d.estado()) || "ASIGNADA".equalsIgnoreCase(d.estado()))
        .toList();

    if (activas.isEmpty()) {
      model.addAttribute("entregas", java.util.List.of());
      return "entidad/entregas";
    }

    java.util.List<Long> ids = activas.stream()
        .map(ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionAsignadaDTO::id)
        .toList();

    var descPorId = activas.stream()
        .collect(java.util.stream.Collectors.toMap(
            ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionAsignadaDTO::id,
            ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionAsignadaDTO::descripcion,
            (a, b) -> a));

    var seguimientos = entidadApiService.entregas(ids);
    var entregasVista = seguimientos.stream()
        .map(s -> entidadApiService.mapearEntrega(s, descPorId.get(s.donacionId())))
        .toList();

    model.addAttribute("entregas", entregasVista);
    return "entidad/entregas";
  }

  /* ---------- CU3: detalle y confirmación de recepción (nivel 3) ---------- */

  @GetMapping("/donaciones/{id}")
  public String detalle(@PathVariable Long id, HttpSession session, Model model) {
    UsuarioActual yo = exigirEntidad(session);
    if (yo == null) {
      return "redirect:/login";
    }

    var optDonacion = entidadApiService.donacion(id);
    if (optDonacion.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Donación inexistente");
    }

    var d = optDonacion.get();
    if (d.entidadId() != null && !java.util.Objects.equals(yo.id(), d.entidadId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esa donación no está asignada a tu entidad");
    }

    model.addAttribute("donacion", entidadApiService.mapearDetalle(d));

    var seguimientos = entidadApiService.entregas(java.util.List.of(id));
    var entregaVista = seguimientos.isEmpty()
        ? null
        : entidadApiService.mapearEntrega(seguimientos.get(0), d.descripcion());
    model.addAttribute("entrega", entregaVista);
    model.addAttribute("form", new ConfirmacionRecepcionForm());
    return "entidad/donacion";
  }


  @PostMapping("/donaciones/{id}/confirmar")
  public String confirmarRecepcion(@PathVariable Long id,
                                   @ModelAttribute("form") ConfirmacionRecepcionForm form,
                                   HttpSession session,
                                   RedirectAttributes redirect) {
    if (exigirEntidad(session) == null) {
      return "redirect:/login";
    }
    int fotos = form.getFotos() == null ? 0
        : (int) java.util.Arrays.stream(form.getFotos()).filter(f -> f != null && !f.isEmpty()).count();
    if (fotos == 0) {
      redirect.addFlashAttribute("toast",
          Toast.error("Adjuntá al menos una foto de lo que recibiste para poder confirmar."));
      return "redirect:/entidad/donaciones/" + id;
    }

    java.util.List<String> fotosGuardadas;
    try {
      fotosGuardadas = fotoRecepcionStorage.guardarFotos(form.getFotos());
    } catch (IllegalArgumentException e) {
      redirect.addFlashAttribute("toast", Toast.error(e.getMessage()));
      return "redirect:/entidad/donaciones/" + id;
    } catch (Exception e) {
      redirect.addFlashAttribute("toast", Toast.error("Error al almacenar las fotos: " + e.getMessage()));
      return "redirect:/entidad/donaciones/" + id;
    }

    var res = entidadApiService.confirmarRecepcion(
        id,
        fotosGuardadas,
        form.getObservaciones(),
        form.isCompleta()
    );

    if (!res.exito()) {
      fotoRecepcionStorage.eliminarFotos(fotosGuardadas);
      redirect.addFlashAttribute("toast", Toast.error(res.mensaje()));
      return "redirect:/entidad/donaciones/" + id;
    }

    redirect.addFlashAttribute("toast",
        Toast.exito("Confirmación registrada. El estado se actualiza en unos segundos."));
    return "redirect:/entidad/dashboard";
  }
}
