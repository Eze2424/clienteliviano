package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista;
import ar.edu.utn.frba.ddsi.clienteliviano.services.DonanteApiService;
import ar.edu.utn.frba.ddsi.clienteliviano.web.Sesion;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ActividadResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DashboardDonanteResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.InsigniaResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.MisionEnCursoResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NotificacionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.NotificacionVista;

@Controller
@RequestMapping("/donante")
public class DonanteController {

  private final Sesion sesion;
  private final DonanteApiService api;
  private final RestTemplate restTemplate;

  @Value("${backend.api.url.donaciones:http://localhost:8080/donaciones-service}")
  private String donacionesUrl;

  @Value("${backend.api.url.incentivos:http://localhost:8082/incentivos-service}")
  private String incentivosUrl;

  @Value("${backend.api.url.notificaciones:http://localhost:8081/notificaciones}")
  private String notificacionesUrl;

  public DonanteController(Sesion sesion, DonanteApiService api, RestTemplate restTemplate) {
    this.sesion = sesion;
    this.api = api;
    this.restTemplate = restTemplate; // Trae el interceptor que propaga el JWT
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

    Long donanteId = yo.id(); // ID de MySQL resuelto para este usuario
    String estadoFiltro = (estado != null && !estado.isBlank()) ? estado.trim() : null;

    // 1. Datos de Donaciones desde donaciones-service
    var resumen = api.dashboard();
    if (resumen.isPresent()) {
      List<DonacionVista> todas = api.aVista(resumen.get().getDonacionesRecientes());
      List<DonacionVista> filtradas = todas;
      if (estadoFiltro != null) {
        filtradas = todas.stream()
            .filter(d -> coincideEstado(d, estadoFiltro))
            .toList();
      }
      model.addAttribute("donaciones", filtradas);
      model.addAttribute("totalDonaciones", resumen.get().getTotalDonaciones());
      model.addAttribute("donacionesEntregadas", resumen.get().getDonacionesEntregadas());
      model.addAttribute("entidadesAyudadas", resumen.get().getOngsBeneficiadas());
    } else {
      model.addAttribute("donaciones", List.of());
      model.addAttribute("totalDonaciones", 0);
      model.addAttribute("donacionesEntregadas", 0);
      model.addAttribute("entidadesAyudadas", 0);
    }

    // 2. Datos reales de Gamificación desde incentivos-service
    try {
      var mision = restTemplate.getForObject(incentivosUrl + "/" + donanteId + "/mision-en-curso", MisionEnCursoResponse.class);
      model.addAttribute("mision", mision);
    } catch (Exception e) {
      model.addAttribute("mision", null);
    }

    try {
      var insignias = restTemplate.getForObject(incentivosUrl + "/" + donanteId + "/insignias", InsigniaResponse[].class);
      model.addAttribute("insignias", insignias != null ? List.of(insignias) : List.of());
    } catch (Exception e) {
      model.addAttribute("insignias", List.of());
    }

    ActividadResponse actividad = null;
    try {
      actividad = restTemplate.getForObject(incentivosUrl + "/" + donanteId + "/actividad", ActividadResponse.class);
      model.addAttribute("actividad", actividad != null ? actividad : new ActividadResponse(0, 0, 0, 0, null, 0, 0L, Map.of(), "COLABORADOR"));
    } catch (Exception e) {
      actividad = new ActividadResponse(0, 0, 0, 0, null, 0, 0L, Map.of(), "COLABORADOR");
      model.addAttribute("actividad", actividad);
    }

    // 3. Notificaciones reales para el donante
    List<NotificacionVista> notificaciones = obtenerNotificaciones(yo, resumen.orElse(null), actividad);
    model.addAttribute("notificaciones", notificaciones);
    long sinLeer = notificaciones.stream().filter(n -> !n.leida()).count();
    model.addAttribute("sinLeer", sinLeer);

    model.addAttribute("estadoFiltro", estadoFiltro);

    return "donante/dashboard";
  }

  private List<NotificacionVista> obtenerNotificaciones(UsuarioActual yo, DashboardDonanteResponse resumen, ActividadResponse actividad) {
    List<NotificacionVista> notificaciones = new ArrayList<>();

    // 1. Consulta primaria a notificaciones-service
    try {
      String url = notificacionesUrl + "?email=" + yo.email() + "&destinatarioId=" + yo.id();
      NotificacionResponse[] desdeServicio = restTemplate.getForObject(url, NotificacionResponse[].class);
      if (desdeServicio != null && desdeServicio.length > 0) {
        for (NotificacionResponse n : desdeServicio) {
          notificaciones.add(new NotificacionVista(
              n.tipo(),
              n.titulo(),
              n.detalle(),
              n.fecha(),
              n.leida()
          ));
        }
        return notificaciones;
      }
    } catch (Exception ignored) {
      // notificaciones-service no disponible o sin registros
    }

    // 2. Si no hay notificaciones almacenadas en el servicio, derivamos avisos según el estado real del donante
    if (resumen != null && resumen.getDonacionesRecientes() != null) {
      for (var d : resumen.getDonacionesRecientes()) {
        if (d.getEntidadNombre() != null && !d.getEntidadNombre().isBlank() && !d.getEntidadNombre().equalsIgnoreCase("Sin asignar")) {
          notificaciones.add(new NotificacionVista(
              "ASIGNACION_DONANTE",
              "Tu donación encontró destino",
              "La donación #" + d.getId() + " (" + d.getTitulo() + ") fue asignada a " + d.getEntidadNombre() + ".",
              LocalDateTime.now().minusHours(2),
              false
          ));
        }
        if ("ENTREGADA".equalsIgnoreCase(d.getEstado())) {
          notificaciones.add(new NotificacionVista(
              "ENTREGA_EXITOSA_DONANTE",
              "Donación entregada",
              "La donación #" + d.getId() + " fue entregada exitosamente a la entidad beneficiaria.",
              LocalDateTime.now().minusDays(1),
              true
          ));
        }
      }
    }

    if (actividad != null && actividad.misionesCompletadas() > 0) {
      notificaciones.add(new NotificacionVista(
          "MISION_COMPLETADA",
          "Misión completada ✓",
          "¡Felicitaciones! Has completado misiones este período. Tu compromiso hace la diferencia.",
          LocalDateTime.now().minusDays(2),
          true
      ));
    }

    return notificaciones;
  }

  private boolean coincideEstado(DonacionVista donacion, String estadoFiltro) {
    if (donacion == null || estadoFiltro == null) {
      return false;
    }
    if (donacion.subdonaciones() != null && !donacion.subdonaciones().isEmpty()) {
      return donacion.subdonaciones().stream()
          .anyMatch(sub -> coincideEstadoString(sub.estado(), estadoFiltro));
    }
    return coincideEstadoString(donacion.estado(), estadoFiltro);
  }

  private boolean coincideEstadoString(String estadoDonacion, String estadoFiltro) {
    if (estadoDonacion == null || estadoFiltro == null) {
      return false;
    }
    if (estadoDonacion.equalsIgnoreCase(estadoFiltro)) {
      return true;
    }
    // Si se filtra por traslado, incluimos también las listas para despacho
    if ("EN_TRASLADO".equalsIgnoreCase(estadoFiltro) && "LISTA".equalsIgnoreCase(estadoDonacion)) {
      return true;
    }
    return false;
  }

  @GetMapping("/donaciones/{id}")
  public String donacion(@PathVariable Long id, HttpSession session, Model model) {
    UsuarioActual yo = exigirDonante(session);
    if (yo == null) {
      return "redirect:/login";
    }

    try {
      // Consumo real de la donación por ID desde donaciones-service
      DonacionVista donacion = restTemplate.getForObject(donacionesUrl + "/donaciones/" + id, DonacionVista.class);
      if (donacion == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Donación no encontrada");
      }
      model.addAttribute("donacion", donacion);
      model.addAttribute("entrega", null);
      model.addAttribute("entidad", null);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Donación inexistente o error en backend");
    }

    return "donante/donacion";
  }

  @GetMapping("/entidades")
  public String entidades(HttpSession session, Model model,
                          @RequestParam(required = false) String nombre) {
    if (exigirDonante(session) == null) {
      return "redirect:/login";
    }

    try {
      List entidades = restTemplate.getForObject(donacionesUrl + "/entidades", List.class);
      model.addAttribute("entidades", entidades != null ? entidades : List.of());
    } catch (Exception e) {
      model.addAttribute("entidades", List.of());
    }
    model.addAttribute("nombre", nombre);
    return "donante/entidades";
  }

  @GetMapping("/ranking")
  public String ranking(HttpSession session, Model model) {
    if (exigirDonante(session) == null) {
      return "redirect:/login";
    }
    // Ranking se puede poblar cuando expongan el endpoint de incentivos
    model.addAttribute("ranking", List.of());
    model.addAttribute("periodos", List.of("2026-10"));
    model.addAttribute("periodoActual", "2026-10");
    return "donante/ranking";
  }
}