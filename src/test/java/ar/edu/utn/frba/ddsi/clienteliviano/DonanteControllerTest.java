package ar.edu.utn.frba.ddsi.clienteliviano;

import ar.edu.utn.frba.ddsi.clienteliviano.controllers.DonanteController;
import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.EntidadVista;
import ar.edu.utn.frba.ddsi.clienteliviano.services.DonanteApiService;
import ar.edu.utn.frba.ddsi.clienteliviano.web.Sesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

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
}
