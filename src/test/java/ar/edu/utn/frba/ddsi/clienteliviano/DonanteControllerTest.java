package ar.edu.utn.frba.ddsi.clienteliviano;

import ar.edu.utn.frba.ddsi.clienteliviano.controllers.DonanteController;
import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.EntregaVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.EntidadVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.RankingFila;
import ar.edu.utn.frba.ddsi.clienteliviano.services.DonanteApiService;
import ar.edu.utn.frba.ddsi.clienteliviano.web.Sesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class DonanteControllerTest {

  private MockMvc mockMvc;
  private Sesion sesion;
  private DonanteApiService donanteApiService;
  private RestTemplate restTemplate;
  private DonanteController donanteController;

  @BeforeEach
  void setUp() {
    sesion = new Sesion();
    donanteApiService = mock(DonanteApiService.class);
    restTemplate = mock(RestTemplate.class);
    donanteController = new DonanteController(sesion, donanteApiService, restTemplate);
    ReflectionTestUtils.setField(donanteController, "donacionesUrl", "http://localhost:8080/donaciones-service");

    InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
    viewResolver.setPrefix("/templates/");
    viewResolver.setSuffix(".html");

    mockMvc = MockMvcBuilders.standaloneSetup(donanteController)
        .setViewResolvers(viewResolver)
        .build();
  }

  private MockHttpSession sesionDonante() {
    MockHttpSession session = new MockHttpSession();
    UsuarioActual usuario = new UsuarioActual(1L, "Carlos Donante", "carlos@tango.com", Rol.DONANTE);
    session.setAttribute("usuarioActual", usuario);
    session.setAttribute("USUARIO", usuario);
    session.setAttribute("JWT_TOKEN", "token-test");
    return session;
  }

  @Test
  void entidades_SinSesion_RedirigeALogin() throws Exception {
    mockMvc.perform(get("/donante/entidades"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void entidades_ConSesionDonante_RetornaVistaConEntidades() throws Exception {
    EntidadVista e1 = new EntidadVista(
        1L, "Comedor Los Hornos", "Calle 137", "11223344",
        -34.9, -57.9, "Comedor", false, null, "Comedor Los Hornos",
        List.of(new NecesidadResponse(10L, "Alimentos", "Arroz y fideos"))
    );
    when(donanteApiService.obtenerEntidades(null)).thenReturn(List.of(e1));

    mockMvc.perform(get("/donante/entidades").session(sesionDonante()))
        .andExpect(status().isOk())
        .andExpect(view().name("donante/entidades"))
        .andExpect(model().attributeExists("entidades"))
        .andExpect(model().attribute("entidades", List.of(e1)));

    verify(donanteApiService).obtenerEntidades(null);
  }

  @Test
  void entidades_ConFiltroNombre_PasaParametroAApi() throws Exception {
    EntidadVista e2 = new EntidadVista(
        2L, "Escuela Rural 12", "Ruta 2", "11998877",
        -35.0, -58.0, "Escuela", true, null, "Escuela Rural 12",
        List.of()
    );
    when(donanteApiService.obtenerEntidades("Escuela")).thenReturn(List.of(e2));

    mockMvc.perform(get("/donante/entidades")
            .param("nombre", "Escuela")
            .session(sesionDonante()))
        .andExpect(status().isOk())
        .andExpect(view().name("donante/entidades"))
        .andExpect(model().attribute("nombre", "Escuela"))
        .andExpect(model().attribute("entidades", List.of(e2)));

    verify(donanteApiService).obtenerEntidades("Escuela");
  }

  @Test
  void ranking_SinSesion_RedirigeALogin() throws Exception {
    mockMvc.perform(get("/donante/ranking"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void ranking_ConSesionDonante_CargaRankingYPeriodos() throws Exception {
    when(donanteApiService.obtenerPeriodosRanking()).thenReturn(List.of("2026-10", "2026-09"));
    RankingFila f1 = new RankingFila(1, 1L, "Carlos Donante", "HUMANA", "Colaborador", 5L, 2, 600L);
    when(donanteApiService.obtenerRanking("2026-10", 10)).thenReturn(List.of(f1));

    mockMvc.perform(get("/donante/ranking").session(sesionDonante()))
        .andExpect(status().isOk())
        .andExpect(view().name("donante/ranking"))
        .andExpect(model().attributeExists("ranking", "periodos", "periodoActual", "yo"))
        .andExpect(model().attribute("periodoActual", "2026-10"))
        .andExpect(model().attribute("ranking", List.of(f1)));

    verify(donanteApiService).obtenerPeriodosRanking();
    verify(donanteApiService).obtenerRanking("2026-10", 10);
  }

  @Test
  void ranking_ConParamPeriodo_PasaPeriodoAApi() throws Exception {
    when(donanteApiService.obtenerPeriodosRanking()).thenReturn(List.of("2026-10", "2026-09"));
    RankingFila f2 = new RankingFila(1, 2L, "Elena Donante", "HUMANA", "Sostenedor", 10L, 3, 1150L);
    when(donanteApiService.obtenerRanking("2026-09", 10)).thenReturn(List.of(f2));

    mockMvc.perform(get("/donante/ranking")
            .param("periodo", "2026-09")
            .session(sesionDonante()))
        .andExpect(status().isOk())
        .andExpect(view().name("donante/ranking"))
        .andExpect(model().attribute("periodoActual", "2026-09"))
        .andExpect(model().attribute("ranking", List.of(f2)));

    verify(donanteApiService).obtenerRanking("2026-09", 10);
  }

  @Test
  void donacion_SinSesion_RedirigeALogin() throws Exception {
    mockMvc.perform(get("/donante/donaciones/10"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void donacion_ConSesion_ConEntrega_CargaDonacionYEntregaEnModelo() throws Exception {
    DonacionVista d = new DonacionVista(10L, "Alimentos", "EN_TRASLADO", LocalDate.now(), null, false, 1L, "Carlos Donante", null, null, 5, null, List.of());
    when(restTemplate.getForObject("http://localhost:8080/donaciones-service/donaciones/10", DonacionVista.class)).thenReturn(d);

    EntregaVista entrega = new EntregaVista("ent-1", 10L, "Alimentos", "AA123BB", null, "EN_TRASLADO", -34.6037, -58.3816, LocalDateTime.now(), "En camino");
    when(donanteApiService.obtenerEntregaPorDonacion(10L)).thenReturn(Optional.of(entrega));

    mockMvc.perform(get("/donante/donaciones/10").session(sesionDonante()))
        .andExpect(status().isOk())
        .andExpect(view().name("donante/donacion"))
        .andExpect(model().attribute("donacion", d))
        .andExpect(model().attribute("entrega", entrega));

    verify(donanteApiService).obtenerEntregaPorDonacion(10L);
  }

  @Test
  void donacion_ConSesion_SinEntrega_CargaDonacionYEntregaNull() throws Exception {
    DonacionVista d = new DonacionVista(10L, "Alimentos", "EN_DEPOSITO", LocalDate.now(), null, false, 1L, "Carlos Donante", null, null, 5, null, List.of());
    when(restTemplate.getForObject("http://localhost:8080/donaciones-service/donaciones/10", DonacionVista.class)).thenReturn(d);

    when(donanteApiService.obtenerEntregaPorDonacion(10L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/donante/donaciones/10").session(sesionDonante()))
        .andExpect(status().isOk())
        .andExpect(view().name("donante/donacion"))
        .andExpect(model().attribute("donacion", d))
        .andExpect(model().attribute("entrega", (Object) null));

    verify(donanteApiService).obtenerEntregaPorDonacion(10L);
  }

  @Test
  void entregas_SinSesion_RedirigeALogin() throws Exception {
    mockMvc.perform(get("/donante/entregas"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void entregas_ConSesion_RetornaVistaConEntregas() throws Exception {
    EntregaVista e = new EntregaVista("ent-1", 10L, "Alimentos", "AA123BB", null, "EN_TRASLADO", -34.6037, -58.3816, LocalDateTime.now(), "En camino");
    when(donanteApiService.obtenerEntregasActivasDonante()).thenReturn(List.of(e));

    mockMvc.perform(get("/donante/entregas").session(sesionDonante()))
        .andExpect(status().isOk())
        .andExpect(view().name("donante/entregas"))
        .andExpect(model().attribute("entregas", List.of(e)));

    verify(donanteApiService).obtenerEntregasActivasDonante();
  }
}
