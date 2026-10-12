package ar.edu.utn.frba.ddsi.clienteliviano.demo;

import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ActividadResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.AsignacionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.CamionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonanteResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.InsigniaResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.MisionEnCursoResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.PropuestaVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.EntidadVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.EntregaVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.NotificacionVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.RankingFila;
import ar.edu.utn.frba.ddsi.clienteliviano.models.entities.TipoOrganizacion;
import java.time.LocalDate;
        import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * ANDAMIO DE DESARROLLO — no forma parte del entregable final.
 *
 * Datos en memoria para poder ver y probar las plantillas sin levantar los
 * cuatro microservicios ni Keycloak. Cuando existan los servicios cliente que
 * llaman a la API, los controllers dejan de pedirle datos a esta clase y esta
 * clase se borra entera. Por eso vive en su propio paquete `demo`.
 *
 * Las formas replican las de la API real (ver models/dto y models/vista):
 * el reemplazo tiene que ser un cambio de origen, no de estructura.
 */
@Component
public class DatosDemo {

  /* ---------------- Usuarios de prueba, uno por rol ---------------- */

  public UsuarioActual usuario(Rol rol) {
    return switch (rol) {
      case DONANTE -> new UsuarioActual(1L, "Elena Martínez", "elena.martinez@ejemplo.org", Rol.DONANTE);
      case ENTIDAD -> new UsuarioActual(7L, "Comedor Los Hornos", "contacto@loshornos.org.ar", Rol.ENTIDAD);
      case ADMIN -> new UsuarioActual(99L, "Carlos Gómez", "carlos.gomez@donatrack.org", Rol.ADMIN);
    };
  }

  /* ---------------- Donaciones ---------------- */

  private final List<DonacionVista> donaciones = new ArrayList<>(List.of(
      new DonacionVista(1041L, "Alimentos no perecederos: 100 kg de arroz, 50 kg de fideos",
          "EN_TRASLADO", LocalDate.of(2026, 9, 24), LocalDate.of(2027, 3, 1), false,
          1L, "Elena Martínez", 7L, "Comedor Los Hornos", 12, null),
      new DonacionVista(1038L, "Frazadas y ropa de abrigo de invierno",
          "ENTREGADA", LocalDate.of(2026, 9, 12), null, false,
          1L, "Elena Martínez", 7L, "Comedor Los Hornos", 28, null),
      new DonacionVista(1052L, "Cuadernos, lápices y útiles escolares",
          "ASIGNADA", LocalDate.of(2026, 9, 27), null, false,
          1L, "Elena Martínez", 2L, "Escuela Rural N.º 12", 60, null),
      new DonacionVista(1055L, "Leche en polvo x 40 latas",
          "EN_DEPOSITO", LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 5), false,
          3L, "Fundación Caminos", null, null, 40, null),
      new DonacionVista(1049L, "Conservas varias (lote donado por supermercado)",
          "EN_DEPOSITO", LocalDate.of(2026, 8, 30), LocalDate.of(2026, 9, 20), true,
          3L, "Fundación Caminos", null, null, 85, null),
      new DonacionVista(1044L, "Pañales y artículos de higiene infantil",
          "ENTREGA_FALLIDA", LocalDate.of(2026, 9, 18), null, false,
          5L, "Supermercados del Sur", 7L, "Comedor Los Hornos", 30, null),
      new DonacionVista(1057L, "Frazadas polares x 50",
          "LISTA", LocalDate.of(2026, 9, 26), null, false,
          3L, "Fundación Caminos", 7L, "Comedor Los Hornos", 50, null)
  ));

  public List<DonacionVista> todasLasDonaciones() {
    return donaciones;
  }

  public List<DonacionVista> donacionesDelDonante(Long donanteId) {
    return donacionesDelDonante(donanteId, null);
  }

  /**
   * Con `estado`, solo las que estan en ese estado. El filtro se hace aca
   * porque esta clase hace de API: cuando exista el endpoint real, el estado
   * viaja como query param y el filtrado lo resuelve el backend.
   */
  public List<DonacionVista> donacionesDelDonante(Long donanteId, String estado) {
    return donaciones.stream()
        .filter(d -> java.util.Objects.equals(donanteId, d.donanteId()))
        .filter(d -> estado == null || estado.isBlank() || estado.equals(d.estado()))
        .toList();
  }

  /** Metrica que el equipo definio en DashboardDonanteResponse. */
  public int donacionesEntregadas(Long donanteId) {
    return (int) donaciones.stream()
        .filter(d -> java.util.Objects.equals(donanteId, d.donanteId()))
        .filter(d -> "ENTREGADA".equals(d.estado()))
        .count();
  }


  /* ---------------- Entidades beneficiarias ---------------- */

  private final List<EntidadVista> entidades = new ArrayList<>(List.of(
      new EntidadVista(2L, "Escuela Rural N.º 12", "Ruta 8 km 94, Pilar", "+54 11 4555-0112",
          -34.4585, -58.9142, "Escuela", true, null, null,
          List.of(new NecesidadResponse(21L, "Mobiliario", "30 sillas de aula"),
              new NecesidadResponse(22L, "Mobiliario", "10 mesas"))),
      new EntidadVista(7L, "Comedor Los Hornos", "Calle 137 y 66, La Plata", "+54 221 455-0199",
          -34.9611, -57.9877, "Comedor", false, null, null,
          List.of(new NecesidadResponse(31L, "Alimentos", "Aceite, harina y fideos"),
              new NecesidadResponse(32L, "Alimentos", "Leche en polvo"))),
      new EntidadVista(4L, "Hogar de Niños Girasol", "Av. Mitre 2210, Avellaneda", "+54 11 4201-7788",
          -34.6627, -58.3650, "Hogar de niños", true, null, null,
          List.of(new NecesidadResponse(41L, "Higiene", "Pañales talle 3 y 4"),
              new NecesidadResponse(42L, "Higiene", "Papel higiénico"))),
      new EntidadVista(9L, "Merendero El Alba", "Barrio San Martín, Quilmes", "+54 11 4253-6600",
          -34.7206, -58.2543, "Comedor", false, null, null,
          List.of(new NecesidadResponse(51L, "Alimentos", "Cacao y azúcar")))
  ));

  public List<EntidadVista> entidades() {
    return entidades;
  }

  public Optional<EntidadVista> entidad(Long id) {
    return entidades.stream().filter(e -> e.id().equals(id)).findFirst();
  }


  /* ---------------- Notificaciones ---------------- */

  private final List<NotificacionVista> notificacionesEntidad = new ArrayList<>(List.of(
      new NotificacionVista("ASIGNACION_ENTIDAD", "Te asignaron una donación",
          "Donación #1041 — alimentos no perecederos — de Elena Martínez.",
          LocalDateTime.of(2026, 9, 28, 16, 10), false),
      new NotificacionVista("RUTA_INICIADA", "La entrega salió del depósito",
          "El camión AB-123-CD inició el traslado de la donación #1041.",
          LocalDateTime.of(2026, 9, 29, 9, 30), false),
      new NotificacionVista("ENTREGA_EXITOSA_ENTIDAD", "Recepción confirmada",
          "Registramos la recepción de la donación #1038 con 2 fotos adjuntas.",
          LocalDateTime.of(2026, 9, 13, 11, 0), true)
  ));

  private final List<NotificacionVista> notificacionesDonante = new ArrayList<>(List.of(
      new NotificacionVista("ASIGNACION_DONANTE", "Tu donación encontró destino",
          "La donación #1052 fue asignada a Escuela Rural N.º 12.",
          LocalDateTime.of(2026, 9, 27, 18, 40), false),
      new NotificacionVista("MISION_COMPLETADA", "Completaste una misión",
          "«Tres donaciones en un mes» — sumaste la insignia Constancia.",
          LocalDateTime.of(2026, 9, 25, 12, 0), true)
  ));

  public List<NotificacionVista> notificaciones(Rol rol) {
    return switch (rol) {
      case ENTIDAD -> notificacionesEntidad;
      case DONANTE -> notificacionesDonante;
      case ADMIN -> List.of();
    };
  }

  public long sinLeer(Rol rol) {
    return notificaciones(rol).stream().filter(n -> !n.leida()).count();
  }

  /* ---------------- Incentivos (dashboard del donante) ---------------- */

  public ActividadResponse actividad() {
    Map<String, Integer> evolucion = new LinkedHashMap<>();
    evolucion.put("2026-04", 6);
    evolucion.put("2026-05", 9);
    evolucion.put("2026-06", 4);
    evolucion.put("2026-07", 11);
    evolucion.put("2026-08", 8);
    evolucion.put("2026-09", 13);
    return new ActividadResponse(132, 13, 18, 9, "2026-09", 13, 3L, evolucion, "COLABORADOR");
  }

  public MisionEnCursoResponse misionEnCurso() {
    return new MisionEnCursoResponse("Cinco entregas en septiembre", 3, 5);
  }

  public List<InsigniaResponse> insignias() {
    return List.of(
        new InsigniaResponse("Constancia", null,
            LocalDateTime.of(2026, 9, 25, 12, 0), "Tres meses seguidos donando"),
        new InsigniaResponse("Primera entrega", null,
            LocalDateTime.of(2026, 3, 4, 9, 0), "Tu primera donación entregada"),
        new InsigniaResponse("Alcance federal", null,
            LocalDateTime.of(2026, 7, 19, 15, 30), "Ayudaste a entidades de tres provincias")
    );
  }

}

