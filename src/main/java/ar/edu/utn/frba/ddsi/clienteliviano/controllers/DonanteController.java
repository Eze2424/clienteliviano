package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.demo.DatosDemo;
import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import ar.edu.utn.frba.ddsi.clienteliviano.web.Sesion;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Área de la persona donante: sus donaciones, el detalle de cada una,
 * el catálogo de entidades y el ranking.
 */
@Controller
@RequestMapping("/donante")
public class DonanteController {

  private final Sesion sesion;
  private final DatosDemo datos;

  public DonanteController(Sesion sesion, DatosDemo datos) {
    this.sesion = sesion;
    this.datos = datos;
  }

  private UsuarioActual exigirDonante(HttpSession session) {
    return sesion.tieneRol(session, Rol.DONANTE) ? sesion.usuario(session) : null;
  }

  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model) {
    UsuarioActual yo = exigirDonante(session);
    if (yo == null) {
      return "redirect:/login";
    }
    // BRECHA G1: no hay endpoint que liste las donaciones de un donante.
    // GET /donaciones es solo ADMIN y devuelve todas.
    model.addAttribute("donaciones", datos.donacionesDelDonante(yo.id()));
    model.addAttribute("actividad", datos.actividad());
    model.addAttribute("mision", datos.misionEnCurso());
    model.addAttribute("insignias", datos.insignias());
    model.addAttribute("notificaciones", datos.notificaciones(Rol.DONANTE));
    return "donante/dashboard";
  }

  @GetMapping("/donaciones/{id}")
  public String donacion(@PathVariable Long id, HttpSession session, Model model) {
    UsuarioActual yo = exigirDonante(session);
    if (yo == null) {
      return "redirect:/login";
    }
    var donacion = datos.donacion(id).filter(d -> yo.id().equals(d.donanteId()));
    if (donacion.isEmpty()) {
      // 404 de verdad: devolver la vista con estado 200 le miente al navegador,
      // a los buscadores y a las herramientas de verificación.
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Donación inexistente o ajena");
    }
    model.addAttribute("donacion", donacion.get());
    model.addAttribute("entrega", donacion.get().entidadId() == null ? null
        : datos.entregasDeLaEntidad(donacion.get().entidadId()).stream()
            .filter(e -> e.donacionId().equals(id)).findFirst().orElse(null));
    model.addAttribute("entidad", donacion.get().entidadId() == null ? null
        : datos.entidad(donacion.get().entidadId()).orElse(null));
    return "donante/donacion";
  }

  @GetMapping("/entidades")
  public String entidades(HttpSession session, Model model,
                          @RequestParam(required = false) String nombre) {
    if (exigirDonante(session) == null) {
      return "redirect:/login";
    }
    // El filtro por nombre lo resolverá la API con un query param; acá solo
    // se conserva lo tipeado para que el campo no se vacíe al recargar.
    model.addAttribute("entidades", datos.entidades());
    model.addAttribute("nombre", nombre);
    return "donante/entidades";
  }

  @GetMapping("/ranking")
  public String ranking(HttpSession session, Model model) {
    if (exigirDonante(session) == null) {
      return "redirect:/login";
    }
    // BRECHA G4: incentivos-service define RankingMensualResponse y una interfaz
    // RankingService, pero no hay implementación ni endpoint que los exponga.
    model.addAttribute("ranking", datos.ranking());
    model.addAttribute("periodos", datos.periodosDeRanking());
    model.addAttribute("periodoActual", datos.periodosDeRanking().get(0));
    return "donante/ranking";
  }
}
