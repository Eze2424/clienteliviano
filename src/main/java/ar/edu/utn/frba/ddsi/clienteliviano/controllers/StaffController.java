package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.demo.DatosDemo;
import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.Toast;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.CamionForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonanteAltaForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.entities.TipoOrganizacion;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Área de la persona administradora (rol ADMIN en Keycloak). Cubre sus seis
 * casos de uso:
 *   1. registrar donantes y donaciones del depósito -> /staff/donantes y /staff/donaciones/nueva
 *   2. marcar donaciones vencidas                   -> /staff/dashboard
 *   3. elegir la entidad destino de una donación    -> /staff/donaciones/{id}/asignar
 *   4. administrar camiones                         -> /staff/camiones
 *   5. ranking mensual e histórico                  -> /staff/ranking
 *   6. importar donantes desde CSV                  -> /staff/donantes
 */
@Controller
@RequestMapping("/staff")
public class StaffController {

  private final Sesion sesion;
  private final DatosDemo datos;

  public StaffController(Sesion sesion, DatosDemo datos) {
    this.sesion = sesion;
    this.datos = datos;
  }

  private boolean esAdmin(HttpSession session) {
    return sesion.tieneRol(session, Rol.ADMIN);
  }

  /* ---------- CU2 y CU3: depósito ---------- */

  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    model.addAttribute("donaciones", datos.todasLasDonaciones());
    model.addAttribute("pendientes", datos.pendientesDeAsignacion());
    model.addAttribute("camiones", datos.camiones());
    return "staff/dashboard";
  }

  @PostMapping("/donaciones/{id}/vencida")
  public String marcarVencida(@PathVariable Long id, HttpSession session,
                              RedirectAttributes redirect) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    // PENDIENTE (equipo): el cambio de estado lo decide la API.
    // EstadosController de donaciones-service está entero comentado (brecha G3).
    redirect.addFlashAttribute("toast",
        Toast.exito("Marcamos la donación #" + id + " como vencida. Sale del circuito de asignación."));
    return "redirect:/staff/dashboard";
  }

  @GetMapping("/donaciones/{id}/asignar")
  public String asignar(@PathVariable Long id, HttpSession session, Model model) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    var donacion = datos.donacion(id);
    if (donacion.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Donación inexistente");
    }
    model.addAttribute("donacion", donacion.get());
    // GET /donaciones-service/{id}/propuestas: el resultado de los algoritmos
    // de selección. El cliente solo lo muestra; la elección es de la persona.
    model.addAttribute("propuestas", datos.propuestas(id));
    return "staff/asignar";
  }

  @PostMapping("/donaciones/{id}/asignar")
  public String confirmarAsignacion(@PathVariable Long id,
                                    @RequestParam Long necesidadId,
                                    @RequestParam String entidad,
                                    HttpSession session,
                                    RedirectAttributes redirect) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    // PENDIENTE (equipo): POST /donaciones-service/{id}/confirmar-asignacion
    // con ConfirmarAsignacionRequest(necesidadId).
    redirect.addFlashAttribute("toast",
        Toast.exito("Asignamos la donación #" + id + " a " + entidad + ". Ya le avisamos."));
    return "redirect:/staff/dashboard";
  }

  /* ---------- CU1: alta de donación ---------- */

  @GetMapping("/donaciones/nueva")
  public String nuevaDonacion(HttpSession session, Model model) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    model.addAttribute("form", new DonacionForm());
    model.addAttribute("donantes", datos.donantes());
    return "staff/nueva-donacion";
  }

  @PostMapping("/donaciones/nueva")
  public String registrarDonacion(@ModelAttribute("form") DonacionForm form,
                                  HttpSession session, Model model,
                                  RedirectAttributes redirect) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    if (form.getDonanteId() == null) {
      model.addAttribute("donantes", datos.donantes());
      model.addAttribute("error", "Elegí a quién pertenece la donación antes de registrarla.");
      return "staff/nueva-donacion";
    }
    // PENDIENTE (equipo): POST /donaciones-service/donaciones con DonacionCreateRequest.
    redirect.addFlashAttribute("toast",
        Toast.exito("Registramos la donación. Ya entró al circuito de asignación."));
    return "redirect:/staff/dashboard";
  }

  /* ---------- CU1 y CU6: donantes e importación ---------- */

  @GetMapping("/donantes")
  public String donantes(HttpSession session, Model model) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    model.addAttribute("donantes", datos.donantes());
    model.addAttribute("tiposOrganizacion", TipoOrganizacion.values());
    if (!model.containsAttribute("form")) {
      model.addAttribute("form", new DonanteAltaForm());
    }
    return "staff/donantes";
  }

  @PostMapping("/donantes")
  public String altaDonante(@ModelAttribute("form") DonanteAltaForm form,
                            HttpSession session, Model model,
                            RedirectAttributes redirect) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    if (form.getDocumento() == null || form.getDocumento().isBlank()) {
      model.addAttribute("donantes", datos.donantes());
      model.addAttribute("tiposOrganizacion", TipoOrganizacion.values());
      model.addAttribute("error", "El número de documento es obligatorio para dar de alta un donante.");
      return "staff/donantes";
    }
    // PENDIENTE (equipo): POST /donaciones-service/donantes
    redirect.addFlashAttribute("toast", Toast.exito("Dimos de alta al donante."));
    return "redirect:/staff/donantes";
  }

  @PostMapping("/donantes/importar")
  public String importarCsv(@RequestParam("archivo") MultipartFile archivo,
                            HttpSession session, RedirectAttributes redirect) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    if (archivo == null || archivo.isEmpty()) {
      redirect.addFlashAttribute("toast", Toast.error("Elegí un archivo CSV antes de importar."));
      return "redirect:/staff/donantes";
    }
    String nombre = archivo.getOriginalFilename();
    if (nombre == null || !nombre.toLowerCase().endsWith(".csv")) {
      redirect.addFlashAttribute("toast",
          Toast.error("El archivo tiene que ser un CSV. Recibimos: " + nombre));
      return "redirect:/staff/donantes";
    }

    // PENDIENTE (equipo): subir el archivo al backend, que lo procesa en segundo
    // plano (la consigna habla de más de 10.000 filas: no puede ser sincrónico).
    // El cliente solo debería consultar el avance y mostrar el resultado.
    redirect.addFlashAttribute("toast",
        Toast.exito("Subimos «" + nombre + "». Te avisamos cuando termine de procesarse."));
    return "redirect:/staff/donantes";
  }

  /* ---------- CU4: camiones ---------- */

  @GetMapping("/camiones")
  public String camiones(HttpSession session, Model model) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    model.addAttribute("camiones", datos.camiones());
    if (!model.containsAttribute("form")) {
      model.addAttribute("form", new CamionForm());
    }
    return "staff/camiones";
  }

  @PostMapping("/camiones")
  public String altaCamion(@ModelAttribute("form") CamionForm form,
                           HttpSession session, Model model,
                           RedirectAttributes redirect) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    if (form.getPatente() == null || form.getPatente().isBlank()) {
      model.addAttribute("camiones", datos.camiones());
      model.addAttribute("error", "La patente es obligatoria.");
      return "staff/camiones";
    }
    // PENDIENTE (equipo): POST /camiones de logistica-service
    redirect.addFlashAttribute("toast",
        Toast.exito("Agregamos el camión " + form.getPatente() + " a la flota."));
    return "redirect:/staff/camiones";
  }

  @PostMapping("/camiones/{id}/eliminar")
  public String bajaCamion(@PathVariable String id, HttpSession session,
                           RedirectAttributes redirect) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    // PENDIENTE (equipo): DELETE /camiones de logistica-service
    redirect.addFlashAttribute("toast", Toast.exito("Dimos de baja el camión."));
    return "redirect:/staff/camiones";
  }

  /* ---------- CU5: ranking ---------- */

  @GetMapping("/ranking")
  public String ranking(HttpSession session, Model model,
                        @RequestParam(required = false) String periodo) {
    if (!esAdmin(session)) {
      return "redirect:/login";
    }
    String elegido = periodo != null ? periodo : datos.periodosDeRanking().get(0);
    model.addAttribute("ranking", datos.ranking());
    model.addAttribute("periodos", datos.periodosDeRanking());
    model.addAttribute("periodoActual", elegido);
    return "staff/ranking";
  }
}
