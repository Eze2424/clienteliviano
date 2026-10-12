package ar.edu.utn.frba.ddsi.clienteliviano;

import ar.edu.utn.frba.ddsi.clienteliviano.controllers.StaffController;
import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.AsignacionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.CamionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.PropuestaVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.RankingFila;
import ar.edu.utn.frba.ddsi.clienteliviano.services.StaffApiService;
import ar.edu.utn.frba.ddsi.clienteliviano.web.Sesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class StaffControllerTest {

  private MockMvc mockMvc;
  private Sesion sesion;
  private RestTemplate restTemplate;
  private StaffApiService staffApiService;
  private StaffController staffController;

  @BeforeEach
  void setUp() {
    sesion = new Sesion();
    restTemplate = mock(RestTemplate.class);
    staffApiService = mock(StaffApiService.class);
    staffController = new StaffController(sesion, restTemplate, staffApiService);

    InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
    viewResolver.setPrefix("/templates/");
    viewResolver.setSuffix(".html");

    mockMvc = MockMvcBuilders.standaloneSetup(staffController)
        .setViewResolvers(viewResolver)
        .build();
  }

  private MockHttpSession sesionConUsuario(Rol rol, Long id) {
    MockHttpSession session = new MockHttpSession();
    UsuarioActual usuario = new UsuarioActual(id, "Admin Test", "admin@test.com", rol);
    session.setAttribute("usuarioActual", usuario);
    session.setAttribute("USUARIO", usuario);
    return session;
  }

  @Test
  void dashboard_sinSesion_redirigeALogin() throws Exception {
    mockMvc.perform(get("/staff/dashboard"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void dashboard_rolDonante_redirigeALogin() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.DONANTE, 1L);

    mockMvc.perform(get("/staff/dashboard").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void dashboard_rolAdmin_retorna200() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.camiones()).thenReturn(List.of());
    when(staffApiService.donacionesIndependientes(any(), anyInt(), anyInt()))
        .thenReturn(new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.PageResponse<>(List.of(), 0, 20, 0, 0));

    mockMvc.perform(get("/staff/dashboard").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("staff/dashboard"))
        .andExpect(model().attributeExists("donaciones", "pagina", "camiones", "sinAsignar", "totalRegistradas", "vencidasTotal"));
  }

  @Test
  void donantes_conAdmin_retornaVistaDonantesYPagina() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.buscarDonantes(anyInt(), anyInt(), any()))
        .thenReturn(new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.PageResponse<>(List.of(), 0, 15, 0, 0));

    mockMvc.perform(get("/staff/donantes").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("staff/donantes"))
        .andExpect(model().attributeExists("donantes", "pagina", "form"));
  }

  @Test
  void altaDonante_ok_redirigeADonantes() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.crearDonante(any())).thenReturn(ResultadoOperacion.ok("Donante creado exitosamente"));

    mockMvc.perform(post("/staff/donantes")
            .session(session)
            .param("nombre", "Juan")
            .param("apellido", "Perez")
            .param("documento", "35123456")
            .param("email", "juan@test.com")
            .param("crearUsuario", "true"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/donantes"))
        .andExpect(flash().attributeExists("toast"));
  }

  @Test
  void altaDonante_error_vuelveAFormularioConError() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.crearDonante(any())).thenReturn(ResultadoOperacion.error("El email ya está registrado"));

    mockMvc.perform(post("/staff/donantes")
            .session(session)
            .param("nombre", "Juan")
            .param("apellido", "Perez")
            .param("documento", "35123456")
            .param("email", "juan@test.com"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/donantes"))
        .andExpect(flash().attributeExists("toast", "form"));
  }

  @Test
  void nuevaDonacion_get_retornaVistaConCategorias() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.subcategorias()).thenReturn(List.of());
    when(staffApiService.buscarDonantes(anyInt(), anyInt(), any()))
        .thenReturn(new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.PageResponse<>(List.of(), 0, 20, 0, 0));

    mockMvc.perform(get("/staff/donaciones/nueva").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("staff/nueva-donacion"))
        .andExpect(model().attributeExists("form", "subcategorias", "donantes"));
  }

  @Test
  void nuevaDonacion_post_ok_redirigeADashboard() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.crearDonacion(any())).thenReturn(ResultadoOperacion.ok("Donación registrada con éxito."));

    mockMvc.perform(post("/staff/donaciones/nueva")
            .session(session)
            .param("donanteId", "1")
            .param("bienes[0].descripcion", "Fideos")
            .param("bienes[0].cantidad", "5")
            .param("bienes[0].subcategoriaId", "10"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/dashboard"))
        .andExpect(flash().attributeExists("toast"));
  }

  @Test
  void marcarVencida_ok_redirigeADashboard() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.marcarDonacionVencida(1L)).thenReturn(ResultadoOperacion.ok("Donación #1 marcada como VENCIDA."));

    mockMvc.perform(post("/staff/donaciones/1/vencida").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/dashboard"))
        .andExpect(flash().attributeExists("toast"));
  }

  @Test
  void camiones_sinAdmin_redirigeALogin() throws Exception {
    mockMvc.perform(get("/staff/camiones"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void camiones_conAdmin_retornaVistaCamiones() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    CamionResponse c = new CamionResponse("id123", "AA111BB", 20.0, 3.0, 5000.0, 0, 0, LocalDateTime.now());
    when(staffApiService.camiones()).thenReturn(List.of(c));

    mockMvc.perform(get("/staff/camiones").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("staff/camiones"))
        .andExpect(model().attributeExists("camiones", "form", "editando"));
  }

  @Test
  void camiones_conEditarId_precargaFormulario() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    CamionResponse c = new CamionResponse("id123", "AA111BB", 20.0, 3.0, 5000.0, 0, 0, LocalDateTime.now());
    when(staffApiService.camiones()).thenReturn(List.of(c));
    when(staffApiService.camionPorId("id123")).thenReturn(Optional.of(c));

    mockMvc.perform(get("/staff/camiones").session(session).param("editarId", "id123"))
        .andExpect(status().isOk())
        .andExpect(view().name("staff/camiones"))
        .andExpect(model().attribute("editando", true));
  }

  @Test
  void altaCamion_ok_agregaToastYRedirige() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.crearCamion(any())).thenReturn(ResultadoOperacion.ok("Agregamos el camión AA111BB a la flota."));

    mockMvc.perform(post("/staff/camiones")
            .session(session)
            .param("patente", "AA111BB")
            .param("capacidadDeCarga", "5000")
            .param("volumen", "20")
            .param("altura", "3"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/camiones"))
        .andExpect(flash().attributeExists("toast"));
  }

  @Test
  void altaCamion_error_agregaToastErrorYRedirige() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.crearCamion(any())).thenReturn(ResultadoOperacion.error("La patente es obligatoria."));

    mockMvc.perform(post("/staff/camiones")
            .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/camiones"))
        .andExpect(flash().attributeExists("toast"));
  }

  @Test
  void editarCamion_ok_agregaToastYRedirige() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.actualizarCamion(eq("id123"), any())).thenReturn(ResultadoOperacion.ok("Actualizamos el camión."));

    mockMvc.perform(post("/staff/camiones/id123")
            .session(session)
            .param("patente", "AA111BB")
            .param("capacidadDeCarga", "5500")
            .param("volumen", "22")
            .param("altura", "3.2"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/camiones"))
        .andExpect(flash().attributeExists("toast"));
  }

  @Test
  void bajaCamion_ok_agregaToastYRedirige() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.eliminarCamion("id123")).thenReturn(ResultadoOperacion.ok("Dimos de baja el camión."));

    mockMvc.perform(post("/staff/camiones/id123/eliminar").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/camiones"))
        .andExpect(flash().attributeExists("toast"));
  }

  @Test
  void asignar_get_conPropuestas_retornaVistaAsignar() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    DonacionVista d = new DonacionVista(10L, "Fideos", "EN_DEPOSITO", null, null, false, 1L, "Donante", null, null, 1, null, List.of());
    PropuestaVista p = new PropuestaVista(101L, 5L, "Comedor", "Comedor", "Alimentos", "Fideos", 10.0, false);
    AsignacionResponse resp = new AsignacionResponse(10L, List.of(p), List.of(), List.of(), true);

    when(staffApiService.obtenerDonacionDetalle(10L)).thenReturn(Optional.of(d));
    when(staffApiService.obtenerPropuestas(10L)).thenReturn(Optional.of(resp));

    mockMvc.perform(get("/staff/donaciones/10/asignar").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("staff/asignar"))
        .andExpect(model().attributeExists("donacion", "propuestas"));
  }

  @Test
  void asignar_get_sinPropuestas_noFallaYRetornaVista() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    DonacionVista d = new DonacionVista(10L, "Fideos", "EN_DEPOSITO", null, null, false, 1L, "Donante", null, null, 1, null, List.of());

    when(staffApiService.obtenerDonacionDetalle(10L)).thenReturn(Optional.of(d));
    when(staffApiService.obtenerPropuestas(10L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/staff/donaciones/10/asignar").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("staff/asignar"))
        .andExpect(model().attributeExists("donacion", "propuestas"));
  }

  @Test
  void asignar_get_donacionInexistente_lanza404() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.obtenerDonacionDetalle(999L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/staff/donaciones/999/asignar").session(session))
        .andExpect(status().isNotFound());
  }

  @Test
  void ejecutarAlgoritmos_post_redirigeAAsignarConToast() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.ejecutarAlgoritmos(10L)).thenReturn(ResultadoOperacion.ok("Algoritmos ejecutados con éxito."));

    mockMvc.perform(post("/staff/donaciones/10/ejecutar-algoritmos").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/donaciones/10/asignar"))
        .andExpect(flash().attributeExists("toast"));
  }

  @Test
  void confirmarAsignacion_post_exito_redirigeADashboard() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.confirmarAsignacion(10L, 101L)).thenReturn(ResultadoOperacion.ok("Asignada con éxito."));

    mockMvc.perform(post("/staff/donaciones/10/asignar")
            .session(session)
            .param("necesidadId", "101"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/dashboard"))
        .andExpect(flash().attributeExists("toast"));
  }

  @Test
  void confirmarAsignacion_post_error_redirigeAAsignarConToast() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.confirmarAsignacion(10L, 101L)).thenReturn(ResultadoOperacion.error("La necesidad no coincide con las propuestas."));

    mockMvc.perform(post("/staff/donaciones/10/asignar")
            .session(session)
            .param("necesidadId", "101"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/donaciones/10/asignar"))
        .andExpect(flash().attributeExists("toast"));
  }

  @Test
  void ranking_sinAdmin_redirigeALogin() throws Exception {
    mockMvc.perform(get("/staff/ranking"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void ranking_conAdmin_retornaVistaConRankingYPeriodos() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    RankingFila fila = new RankingFila(1, 10L, "Donante Top", "HUMANA", "Guardián", 15, 5, 1750);
    when(staffApiService.obtenerPeriodosRanking()).thenReturn(List.of("2026-10", "2026-09"));
    when(staffApiService.obtenerRanking("2026-10", 10)).thenReturn(List.of(fila));

    mockMvc.perform(get("/staff/ranking").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("staff/ranking"))
        .andExpect(model().attributeExists("ranking", "periodos", "periodoActual"))
        .andExpect(model().attribute("periodoActual", "2026-10"));
  }

  @Test
  void ranking_conPeriodoEspecifico_cargaEsePeriodo() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    when(staffApiService.obtenerPeriodosRanking()).thenReturn(List.of("2026-10", "2026-09"));
    when(staffApiService.obtenerRanking("2026-09", 10)).thenReturn(List.of());

    mockMvc.perform(get("/staff/ranking").session(session).param("periodo", "2026-09"))
        .andExpect(status().isOk())
        .andExpect(view().name("staff/ranking"))
        .andExpect(model().attribute("periodoActual", "2026-09"));
  }

  @Test
  void importarCsv_conAdmin_exito_redirigeAProgreso() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
        "archivo", "donantes.csv", "text/csv", "HUMANA|DNI|123|Juan|j@t.com|11".getBytes()
    );
    when(staffApiService.iniciarImportacionCsv(any())).thenReturn(
        ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoImportacion.ok("Iniciado", "job-xyz")
    );

    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/staff/donantes/importar")
            .file(file)
            .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/donantes/importar?jobId=job-xyz"));
  }

  @Test
  void importarCsv_conAdmin_error_redirigeADonantesConToast() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
        "archivo", "donantes.csv", "text/csv", "datos".getBytes()
    );
    when(staffApiService.iniciarImportacionCsv(any())).thenReturn(
        ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoImportacion.error("Fallo de conexión")
    );

    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/staff/donantes/importar")
            .file(file)
            .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/donantes"))
        .andExpect(flash().attributeExists("toast"));
  }

  @Test
  void verProgresoImportacion_conAdmin_retornaVista() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    var job = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ImportacionJobResponse(
        "job-xyz", "EN_CURSO", 50, 100, 40, 10, List.of(), "Procesando"
    );
    when(staffApiService.consultarEstadoImportacion("job-xyz")).thenReturn(Optional.of(job));

    mockMvc.perform(get("/staff/donantes/importar").session(session).param("jobId", "job-xyz"))
        .andExpect(status().isOk())
        .andExpect(view().name("staff/importar"))
        .andExpect(model().attributeExists("job"));
  }

  @Test
  void consultarEstadoApi_conAdmin_retornaJson() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ADMIN, 99L);
    var job = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ImportacionJobResponse(
        "job-xyz", "FINALIZADO", 100, 100, 80, 20, List.of(), "Finalizado"
    );
    when(staffApiService.consultarEstadoImportacion("job-xyz")).thenReturn(Optional.of(job));

    mockMvc.perform(get("/staff/api/donantes/importar/job-xyz").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.jobId").value("job-xyz"))
        .andExpect(jsonPath("$.estado").value("FINALIZADO"))
        .andExpect(jsonPath("$.creados").value(80));
  }
}

