package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.demo.DatosDemo;
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
  private final DatosDemo datos;

  public EntidadController(Sesion sesion, DatosDemo datos) {
    this.sesion = sesion;
    this.datos = datos;
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
    model.addAttribute("donaciones", datos.donacionesDeLaEntidad(yo.id()));
    model.addAttribute("notificaciones", datos.notificaciones(Rol.ENTIDAD));
    model.addAttribute("entregasActivas", datos.entregasDeLaEntidad(yo.id()).size());
    model.addAttribute("necesidadesActivas", datos.necesidadesPropias().size());
    return "entidad/dashboard";
  }

  /* ---------- CU1: registrar necesidades materiales ---------- */

  @GetMapping("/necesidades")
  public String necesidades(HttpSession session, Model model) {
    if (exigirEntidad(session) == null) {
      return "redirect:/login";
    }
    model.addAttribute("necesidades", datos.necesidadesPropias());
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
    if (exigirEntidad(session) == null) {
      return "redirect:/login";
    }
    // Validación de formato, para dar feedback sin ida y vuelta al servidor.
    // Las reglas de dominio (subcategoría válida, tope de cantidad) son de la API.
    if (form.getDescripcion() == null || form.getDescripcion().isBlank()) {
      model.addAttribute("necesidades", datos.necesidadesPropias());
      model.addAttribute("error", "Contanos qué necesitás: la descripción no puede quedar vacía.");
      return "entidad/necesidades";
    }

    // PENDIENTE (equipo): POST {donaciones}/entidades/{entidadId}/necesidades
    redirect.addFlashAttribute("toast",
        Toast.exito("Registramos la necesidad. Ya entra en las próximas asignaciones."));
    return "redirect:/entidad/necesidades";
  }

  @PostMapping("/necesidades/{id}/eliminar")
  public String eliminarNecesidad(@PathVariable Long id,
                                  HttpSession session,
                                  RedirectAttributes redirect) {
    if (exigirEntidad(session) == null) {
      return "redirect:/login";
    }
    // PENDIENTE (equipo): DELETE {donaciones}/entidades/{entidadId}/necesidades/{id}
    redirect.addFlashAttribute("toast", Toast.exito("Dimos de baja la necesidad."));
    return "redirect:/entidad/necesidades";
  }

  /* ---------- CU5: seguimiento de entregas en el mapa ---------- */

  @GetMapping("/entregas")
  public String entregas(HttpSession session, Model model) {
    UsuarioActual yo = exigirEntidad(session);
    if (yo == null) {
      return "redirect:/login";
    }
    model.addAttribute("entregas", datos.entregasDeLaEntidad(yo.id()));
    return "entidad/entregas";
  }

  /* ---------- CU3: detalle y confirmación de recepción (nivel 3) ---------- */

  @GetMapping("/donaciones/{id}")
  public String detalle(@PathVariable Long id, HttpSession session, Model model) {
    UsuarioActual yo = exigirEntidad(session);
    if (yo == null) {
      return "redirect:/login";
    }
    var donacion = datos.donacion(id).filter(d -> yo.id().equals(d.entidadId()));
    if (donacion.isEmpty()) {
      // 404 de verdad: devolver la vista con estado 200 le miente al navegador,
      // a los buscadores y a las herramientas de verificación.
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Donación inexistente o ajena");
    }
    model.addAttribute("donacion", donacion.get());
    model.addAttribute("entrega", datos.entregasDeLaEntidad(yo.id()).stream()
        .filter(e -> e.donacionId().equals(id)).findFirst().orElse(null));
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
        : (int) java.util.Arrays.stream(form.getFotos()).filter(f -> !f.isEmpty()).count();
    if (fotos == 0) {
      redirect.addFlashAttribute("toast",
          Toast.error("Adjuntá al menos una foto de lo que recibiste para poder confirmar."));
      return "redirect:/entidad/donaciones/" + id;
    }

    // PENDIENTE (equipo): PUT {logistica}/entregas/{idEntrega}/resolucion
    // más la subida de las fotos, que hoy ningún endpoint acepta.
    redirect.addFlashAttribute("toast",
        Toast.exito("Confirmaste la recepción con " + fotos + " foto(s). Avisamos a quien donó."));
    return "redirect:/entidad/dashboard";
  }
}
