package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.demo.DatosDemo;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista;
import ar.edu.utn.frba.ddsi.clienteliviano.services.DonanteApiService;
import java.util.List;
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
  private final DonanteApiService api;

  public DonanteController(Sesion sesion, DatosDemo datos, DonanteApiService api) {
    this.sesion = sesion;
    this.datos = datos;
    this.api = api;
  }

  private UsuarioActual exigirDonante(HttpSession session) {
    return sesion.tieneRol(session, Rol.DONANTE) ? sesion.usuario(session) : null;
  }

  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model,
                          @RequestParam(required = false) String estado) {
    UsuarioActual yo = exigirDonante(session);
    if (yo == null) {
      return "redirect:/login";
    }

    // Endpoint agregado que propuso el equipo: resuelve G1 y G2 de una.
    // Mientras no exista, el andamio ocupa su lugar y la vista no se entera.
    var resumen = api.dashboard();
    List<DonacionVista> donaciones;
    if (resumen.isPresent()) {
      donaciones = api.aVista(resumen.get().getDonacionesRecientes());
      model.addAttribute("totalDonaciones", resumen.get().getTotalDonaciones());
      model.addAttribute("donacionesEntregadas", resumen.get().getDonacionesEntregadas());
      model.addAttribute("entidadesAyudadas", resumen.get().getOngsBeneficiadas());
    } else {
      donaciones = datos.donacionesDelDonante(yo.id(), estado);
      model.addAttribute("totalDonaciones", datos.actividad().totalHistoricoDonaciones());
      model.addAttribute("donacionesEntregadas", datos.donacionesEntregadas(yo.id()));
      model.addAttribute("entidadesAyudadas", datos.actividad().organizacionesAyudadas());
    }

    model.addAttribute("donaciones", donaciones);
    model.addAttribute("estadoFiltro", estado);
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
    var donacion = datos.donacion(id).filter(d -> java.util.Objects.equals(yo.id(), d.donanteId()));
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
