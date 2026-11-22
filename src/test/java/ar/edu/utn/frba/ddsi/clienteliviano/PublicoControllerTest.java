package ar.edu.utn.frba.ddsi.clienteliviano;

import ar.edu.utn.frba.ddsi.clienteliviano.controllers.PublicoController;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionPublicaResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.RegistroForm;
import ar.edu.utn.frba.ddsi.clienteliviano.services.RegistroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PublicoControllerTest {

  private MockMvc mockMvc;
  private RegistroService registroService;
  private RestTemplate restTemplate;
  private PublicoController publicoController;
  private final String donacionesUrl = "http://localhost:8080/donaciones-service";

  @BeforeEach
  void setUp() {
    registroService = mock(RegistroService.class);
    restTemplate = mock(RestTemplate.class);
    publicoController = new PublicoController(registroService, restTemplate);
    ReflectionTestUtils.setField(publicoController, "donacionesUrl", donacionesUrl);

    InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
    viewResolver.setPrefix("/templates/");
    viewResolver.setSuffix(".html");

    mockMvc = MockMvcBuilders.standaloneSetup(publicoController)
        .setViewResolvers(viewResolver)
        .build();
  }

  @Test
  void portada_cargaEntregasPublicas() throws Exception {
    DonacionPublicaResponse entrega = new DonacionPublicaResponse(
        1L,
        "Alimentos no perecederos",
        "Arroz y fideos",
        "Comedor Esperanza",
        "Calle 123",
        -34.6037,
        -58.3816,
        java.time.LocalDate.now(),
        List.of("http://localhost:8086/uploads/recepciones/foto1.jpg"),
        10
    );

    when(restTemplate.getForEntity(donacionesUrl + "/publico/entregadas", DonacionPublicaResponse[].class))
        .thenReturn(new ResponseEntity<>(new DonacionPublicaResponse[]{entrega}, HttpStatus.OK));

    mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("publico/index"))
        .andExpect(model().attributeExists("entregasPublicas"))
        .andExpect(model().attribute("entregasPublicas", hasSize(1)));
  }

  @Test
  void portada_errorBackend_retornaListaVacia() throws Exception {
    when(restTemplate.getForEntity(donacionesUrl + "/publico/entregadas", DonacionPublicaResponse[].class))
        .thenThrow(new RuntimeException("Connection error"));

    mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("publico/index"))
        .andExpect(model().attributeExists("entregasPublicas"))
        .andExpect(model().attribute("entregasPublicas", hasSize(0)));
  }

  @Test
  void legal_retornaVista() throws Exception {
    mockMvc.perform(get("/legal"))
        .andExpect(status().isOk())
        .andExpect(view().name("publico/legal"));
  }

  @Test
  void formularioRegistro_retornaVista() throws Exception {
    mockMvc.perform(get("/registro"))
        .andExpect(status().isOk())
        .andExpect(view().name("publico/registro"))
        .andExpect(model().attributeExists("form", "tiposOrganizacion"));
  }

  @Test
  void registrar_passwordsNoCoinciden_retornaError() throws Exception {
    mockMvc.perform(post("/registro")
            .param("password", "abc12345")
            .param("passwordConfirmacion", "abc12346")
            .param("aceptaTerminos", "true"))
        .andExpect(status().isOk())
        .andExpect(view().name("publico/registro"))
        .andExpect(model().attributeExists("error"));

    verify(registroService, never()).procesarRegistro(any());
  }

  @Test
  void registrar_sinAceptarTerminos_retornaError() throws Exception {
    mockMvc.perform(post("/registro")
            .param("password", "abc12345")
            .param("passwordConfirmacion", "abc12345")
            .param("aceptaTerminos", "false"))
        .andExpect(status().isOk())
        .andExpect(view().name("publico/registro"))
        .andExpect(model().attributeExists("error"));

    verify(registroService, never()).procesarRegistro(any());
  }

  @Test
  void registrar_ok_redirigeALogin() throws Exception {
    mockMvc.perform(post("/registro")
            .param("password", "abc12345")
            .param("passwordConfirmacion", "abc12345")
            .param("aceptaTerminos", "true"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?registroExitoso=true"))
        .andExpect(flash().attributeExists("toast"));

    verify(registroService).procesarRegistro(any(RegistroForm.class));
  }

  @Test
  void deserializacionJsonExactoDonacionesService() throws Exception {
    String jsonExacto = """
        {
          "id": 1,
          "subcategoria": "Alimentos no perecederos",
          "entidadNombre": "Fundacion Medrano",
          "entidadLatitud": -34.59868,
          "entidadLongitud": -58.41995,
          "entidadDireccion": "Medrano 951, Almagro",
          "fechaEntrega": "2026-11-22",
          "cantidadBienes": 5,
          "fotosComprobante": [
            "http://localhost:8086/uploads/recepciones/comprobante1.jpg"
          ]
        }
        """;

    tools.jackson.databind.json.JsonMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();
    DonacionPublicaResponse res = mapper.readValue(jsonExacto, DonacionPublicaResponse.class);

    assertEquals(1L, res.id());
    assertEquals("Alimentos no perecederos", res.subcategoria());
    assertEquals("Alimentos no perecederos", res.descripcion());
    assertEquals("Fundacion Medrano", res.entidadNombre());
    assertEquals("Medrano 951, Almagro", res.entidadDireccion());
    assertEquals(-34.59868, res.latitud(), 0.0001);
    assertEquals(-58.41995, res.longitud(), 0.0001);
    assertEquals(5, res.cantidadBienes());
    assertEquals(1, res.fotos().size());
    assertEquals("http://localhost:8086/uploads/recepciones/comprobante1.jpg", res.fotos().get(0));
  }
}
