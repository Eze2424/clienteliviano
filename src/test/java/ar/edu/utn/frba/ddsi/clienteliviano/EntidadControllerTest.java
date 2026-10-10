package ar.edu.utn.frba.ddsi.clienteliviano;

import ar.edu.utn.frba.ddsi.clienteliviano.controllers.EntidadController;
import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.SubcategoriaResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.services.EntidadApiService;
import ar.edu.utn.frba.ddsi.clienteliviano.web.Sesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class EntidadControllerTest {

  private MockMvc mockMvc;
  private Sesion sesion;
  private EntidadApiService entidadApiService;
  private ar.edu.utn.frba.ddsi.clienteliviano.services.NotificacionApiService notificacionApiService;
  private ar.edu.utn.frba.ddsi.clienteliviano.services.FotoRecepcionStorage fotoRecepcionStorage;
  private EntidadController entidadController;

  @BeforeEach
  void setUp() {
    sesion = new Sesion();
    entidadApiService = mock(EntidadApiService.class);
    notificacionApiService = mock(ar.edu.utn.frba.ddsi.clienteliviano.services.NotificacionApiService.class);
    fotoRecepcionStorage = mock(ar.edu.utn.frba.ddsi.clienteliviano.services.FotoRecepcionStorage.class);
    entidadController = new EntidadController(sesion, entidadApiService, notificacionApiService, fotoRecepcionStorage);

    InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
    viewResolver.setPrefix("/templates/");
    viewResolver.setSuffix(".html");

    mockMvc = MockMvcBuilders.standaloneSetup(entidadController)
        .setViewResolvers(viewResolver)
        .build();
  }

  private MockHttpSession sesionConUsuario(Rol rol, Long id) {
    MockHttpSession session = new MockHttpSession();
    UsuarioActual usuario = new UsuarioActual(id, "Usuario Test", "test@test.com", rol);
    session.setAttribute("usuarioActual", usuario);
    session.setAttribute("USUARIO", usuario);
    return session;
  }

  @Test
  void necesidades_SinSesion_RedirigeALogin() throws Exception {
    mockMvc.perform(get("/entidad/necesidades"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void necesidades_RolDonante_RedirigeALogin() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.DONANTE, 1L);

    mockMvc.perform(get("/entidad/necesidades").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void necesidades_RolEntidad_MuestraVistaYModel() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    when(entidadApiService.necesidades()).thenReturn(List.of(
        new NecesidadResponse(1L, "Alimentos", "Arroz", 10.0, false)
    ));
    when(entidadApiService.subcategorias()).thenReturn(List.of(
        new SubcategoriaResponse(1L, "Arroz", "Alimentos")
    ));

    mockMvc.perform(get("/entidad/necesidades").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("entidad/necesidades"))
        .andExpect(model().attributeExists("necesidades"))
        .andExpect(model().attributeExists("subcategorias"))
        .andExpect(model().attributeExists("form"));
  }

  @Test
  void registrarNecesidad_DescripcionVacia_RetornaVistaConError() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    mockMvc.perform(post("/entidad/necesidades")
            .session(session)
            .param("descripcion", "")
            .param("subcategoriaId", "1")
            .param("cantidadSolicitada", "10"))
        .andExpect(status().isOk())
        .andExpect(view().name("entidad/necesidades"))
        .andExpect(model().attributeExists("error"));

    verify(entidadApiService, never()).registrarNecesidad(any(), any());
  }

  @Test
  void registrarNecesidad_Valida_LlamaApiYRedirigeConToast() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    when(entidadApiService.registrarNecesidad(eq(10L), any(NecesidadForm.class)))
        .thenReturn(ResultadoOperacion.ok("Necesidad registrada correctamente."));

    mockMvc.perform(post("/entidad/necesidades")
            .session(session)
            .param("descripcion", "Harina y azúcar")
            .param("subcategoriaId", "1")
            .param("cantidadSolicitada", "25")
            .param("esExtraordinaria", "false"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/entidad/necesidades"))
        .andExpect(flash().attributeExists("toast"));

    verify(entidadApiService).registrarNecesidad(eq(10L), any(NecesidadForm.class));
  }

  @Test
  void eliminarNecesidad_LlamaApiYRedirige() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    when(entidadApiService.eliminarNecesidad(10L, 50L))
        .thenReturn(ResultadoOperacion.ok("Eliminada"));

    mockMvc.perform(post("/entidad/necesidades/50/eliminar").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/entidad/necesidades"))
        .andExpect(flash().attributeExists("toast"));

    verify(entidadApiService).eliminarNecesidad(10L, 50L);
  }

  @Test
  void dashboard_RespuestaExitosa_MuestraDonacionesYMetricas() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    var donacionDTO = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionAsignadaDTO(
        100L, "Alimentos", "ASIGNADA", java.time.LocalDate.now(), null, "Donante", 1L, 5, List.of()
    );
    var dashResponse = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DashboardEntidadResponse(
        "Hogar", 1, 0, 1, 2, List.of(donacionDTO)
    );

    when(entidadApiService.dashboard()).thenReturn(java.util.Optional.of(dashResponse));
    when(entidadApiService.mapearDonacion(any(), eq(10L))).thenReturn(
        new ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista(
            100L, "Alimentos", "ASIGNADA", java.time.LocalDate.now(), null, false, 1L, "Donante", 10L, "Hogar", 5, null
        )
    );

    mockMvc.perform(get("/entidad/dashboard").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("entidad/dashboard"))
        .andExpect(model().attributeExists("donaciones"))
        .andExpect(model().attribute("entregasActivas", 1))
        .andExpect(model().attribute("necesidadesActivas", 2));
  }

  @Test
  void dashboard_BackendCaido_MuestraMensajeDeError() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    when(entidadApiService.dashboard()).thenReturn(java.util.Optional.empty());

    mockMvc.perform(get("/entidad/dashboard").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("entidad/dashboard"))
        .andExpect(model().attributeExists("error"));
  }

  @Test
  void detalle_DonacionPropia_MuestraDetalle() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    var detalleDTO = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionDetalleDTO(
        500L, "Ropa", "EN_TRASLADO", java.time.LocalDate.now(), null, "Donante", 2L, 10L, "Hogar", 3, List.of()
    );
    when(entidadApiService.donacion(500L)).thenReturn(java.util.Optional.of(detalleDTO));
    when(entidadApiService.mapearDetalle(any())).thenReturn(
        new ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista(
            500L, "Ropa", "EN_TRASLADO", java.time.LocalDate.now(), null, false, 2L, "Donante", 10L, "Hogar", 3, null
        )
    );

    mockMvc.perform(get("/entidad/donaciones/500").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("entidad/donacion"))
        .andExpect(model().attributeExists("donacion"));
  }

  @Test
  void detalle_DonacionInexistente_Retorna404() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    when(entidadApiService.donacion(999L)).thenReturn(java.util.Optional.empty());

    mockMvc.perform(get("/entidad/donaciones/999").session(session))
        .andExpect(status().isNotFound());
  }

  @Test
  void detalle_DonacionAjena_Retorna403() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    var detalleAjeno = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionDetalleDTO(
        600L, "Muebles", "ASIGNADA", java.time.LocalDate.now(), null, "Donante", 2L, 99L, "Otra Entidad", 1, List.of()
    );
    when(entidadApiService.donacion(600L)).thenReturn(java.util.Optional.of(detalleAjeno));

    mockMvc.perform(get("/entidad/donaciones/600").session(session))
        .andExpect(status().isForbidden());
  }

  @Test
  void marcarNotificacionLeida_Exito_RedirigeADashboard() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);
    when(notificacionApiService.marcarLeida(15L)).thenReturn(true);

    mockMvc.perform(post("/entidad/notificaciones/15/leida").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/entidad/dashboard#notificaciones"));

    verify(notificacionApiService).marcarLeida(15L);
  }

  @Test
  void marcarNotificacionLeida_SinSesion_RedirigeALogin() throws Exception {
    mockMvc.perform(post("/entidad/notificaciones/15/leida"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));

    verifyNoInteractions(notificacionApiService);
  }

  @Test
  void marcarNotificacionLeida_Falla_AgregaToastErrorYRedirige() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);
    when(notificacionApiService.marcarLeida(15L)).thenReturn(false);

    mockMvc.perform(post("/entidad/notificaciones/15/leida").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/entidad/dashboard#notificaciones"))
        .andExpect(flash().attributeExists("toast"));
  }

  @Test
  void entregas_SinSesion_RedirigeALogin() throws Exception {
    mockMvc.perform(get("/entidad/entregas"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void entregas_ConEntregasActivas_MuestraVistaConEntregas() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    var donacionDTO = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionAsignadaDTO(
        100L, "Alimentos", "EN_TRASLADO", java.time.LocalDate.now(), null, "Donante", 1L, 5, List.of()
    );
    var dashResponse = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DashboardEntidadResponse(
        "Hogar", 1, 0, 1, 2, List.of(donacionDTO)
    );
    when(entidadApiService.dashboard()).thenReturn(java.util.Optional.of(dashResponse));

    var seguimiento = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.EntregaSeguimientoDTO(
        "uuid-1", 100L, "EN_TRASLADO", "AB123CD", -34.60, -58.38, java.time.LocalDateTime.now()
    );
    when(entidadApiService.entregas(List.of(100L))).thenReturn(List.of(seguimiento));
    when(entidadApiService.mapearEntrega(any(), any())).thenReturn(
        new ar.edu.utn.frba.ddsi.clienteliviano.models.vista.EntregaVista(
            "uuid-1", 100L, "Alimentos", "AB123CD", null, "EN_TRASLADO", -34.60, -58.38, java.time.LocalDateTime.now(), null
        )
    );

    mockMvc.perform(get("/entidad/entregas").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("entidad/entregas"))
        .andExpect(model().attributeExists("entregas"));
  }

  @Test
  void entregas_SinEntregasActivas_MuestraListaVacia() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    var dashResponse = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DashboardEntidadResponse(
        "Hogar", 0, 0, 0, 2, List.of()
    );
    when(entidadApiService.dashboard()).thenReturn(java.util.Optional.of(dashResponse));

    mockMvc.perform(get("/entidad/entregas").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("entidad/entregas"))
        .andExpect(model().attribute("entregas", List.of()));
  }

  @Test
  void entregas_DashboardCaido_MuestraErrorSinRomper() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    when(entidadApiService.dashboard()).thenReturn(java.util.Optional.empty());

    mockMvc.perform(get("/entidad/entregas").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("entidad/entregas"))
        .andExpect(model().attributeExists("error"));
  }

  @Test
  void confirmarRecepcion_SinFotos_ErrorToastYNoLlamaApi() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);

    mockMvc.perform(multipart("/entidad/donaciones/10/confirmar")
            .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/entidad/donaciones/10"))
        .andExpect(flash().attributeExists("toast"));

    verify(entidadApiService, never()).confirmarRecepcion(any(), any(), any(), anyBoolean());
    verify(fotoRecepcionStorage, never()).guardarFotos(any());
  }

  @Test
  void confirmarRecepcion_Exito_GuardaFotosLlamaApiYRedirigeDashboard() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);
    MockMultipartFile foto = new MockMultipartFile("fotos", "recibo.jpg", "image/jpeg", new byte[]{1, 2, 3});

    when(fotoRecepcionStorage.guardarFotos(any())).thenReturn(List.of("http://localhost:8086/uploads/recepciones/recibo.jpg"));
    when(entidadApiService.confirmarRecepcion(eq(10L), any(), any(), eq(true)))
        .thenReturn(ResultadoOperacion.ok("Confirmada"));

    mockMvc.perform(multipart("/entidad/donaciones/10/confirmar")
            .file(foto)
            .param("observaciones", "Todo en orden")
            .param("completa", "true")
            .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/entidad/dashboard"))
        .andExpect(flash().attributeExists("toast"));

    verify(fotoRecepcionStorage).guardarFotos(any());
    verify(entidadApiService).confirmarRecepcion(eq(10L), eq(List.of("http://localhost:8086/uploads/recepciones/recibo.jpg")), eq("Todo en orden"), eq(true));
    verify(fotoRecepcionStorage, never()).eliminarFotos(any());
  }

  @Test
  void confirmarRecepcion_ErrorApi_LimpiaFotosYRedirigeADetalle() throws Exception {
    MockHttpSession session = sesionConUsuario(Rol.ENTIDAD, 10L);
    MockMultipartFile foto = new MockMultipartFile("fotos", "recibo.jpg", "image/jpeg", new byte[]{1, 2, 3});

    when(fotoRecepcionStorage.guardarFotos(any())).thenReturn(List.of("http://localhost:8086/uploads/recepciones/recibo.jpg"));
    when(entidadApiService.confirmarRecepcion(eq(10L), any(), any(), eq(true)))
        .thenReturn(ResultadoOperacion.error("Esa donación no es tuya"));

    mockMvc.perform(multipart("/entidad/donaciones/10/confirmar")
            .file(foto)
            .param("observaciones", "Observacion")
            .param("completa", "true")
            .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/entidad/donaciones/10"))
        .andExpect(flash().attributeExists("toast"));

    verify(fotoRecepcionStorage).eliminarFotos(List.of("http://localhost:8086/uploads/recepciones/recibo.jpg"));
  }
}

