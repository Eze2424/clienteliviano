package ar.edu.utn.frba.ddsi.clienteliviano;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.EntidadResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.services.EntidadApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class EntidadApiServiceTest {

  private RestTemplate restTemplate;
  private MockRestServiceServer mockServer;
  private EntidadApiService entidadApiService;
  private final String baseUrl = "http://localhost:8080/donaciones-service";

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
    entidadApiService = new EntidadApiService(restTemplate);
    ReflectionTestUtils.setField(entidadApiService, "backendApiUrl", baseUrl);
    ReflectionTestUtils.setField(entidadApiService, "logisticaUrl", "http://localhost:8083");
  }

  @Test
  void miEntidad_Respuesta200_RetornaEntidad() {
    String json = """
        {
          "id": 15,
          "razonSocial": "Hogar Santa Rosa",
          "direccion": "Av. Mitre 500",
          "latitud": -34.6037,
          "longitud": -58.3816,
          "telefono": "1144556677"
        }
        """;

    mockServer.expect(requestTo(baseUrl + "/entidades/me"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    Optional<EntidadResponse> opt = entidadApiService.miEntidad();

    assertTrue(opt.isPresent());
    assertEquals(15L, opt.get().id());
    assertEquals("Hogar Santa Rosa", opt.get().razonSocial());
    mockServer.verify();
  }

  @Test
  void miEntidad_Respuesta404_RetornaOptionalVacioSinExcepcion() {
    mockServer.expect(requestTo(baseUrl + "/entidades/me"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    Optional<EntidadResponse> opt = entidadApiService.miEntidad();

    assertTrue(opt.isEmpty());
    mockServer.verify();
  }

  @Test
  void miEntidad_Respuesta403_RetornaOptionalVacioSinExcepcion() {
    mockServer.expect(requestTo(baseUrl + "/entidades/me"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));

    Optional<EntidadResponse> opt = entidadApiService.miEntidad();

    assertTrue(opt.isEmpty());
    mockServer.verify();
  }

  @Test
  void miEntidad_Respuesta500_RetornaOptionalVacioSinExcepcion() {
    mockServer.expect(requestTo(baseUrl + "/entidades/me"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    Optional<EntidadResponse> opt = entidadApiService.miEntidad();

    assertTrue(opt.isEmpty());
    mockServer.verify();
  }

  @Test
  void subcategorias_Respuesta200_RetornaLista() {
    String json = """
        [
          {"id": 1, "nombre": "Arroz", "categoria": "Alimentos"},
          {"id": 2, "nombre": "Fideos", "categoria": "Alimentos"}
        ]
        """;

    mockServer.expect(requestTo(baseUrl + "/subcategorias"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    var lista = entidadApiService.subcategorias();

    assertEquals(2, lista.size());
    assertEquals("Arroz", lista.get(0).nombre());
    mockServer.verify();
  }

  @Test
  void necesidades_Respuesta200_RetornaLista() {
    String json = """
        [
          {"id": 10, "subcategoria": "Arroz", "descripcion": "10 paquetes", "cantidadSolicitada": 10.0, "esExtraordinaria": false}
        ]
        """;

    mockServer.expect(requestTo(baseUrl + "/entidades/me/necesidades"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    var lista = entidadApiService.necesidades();

    assertEquals(1, lista.size());
    assertEquals("Arroz", lista.get(0).subcategoria());
    assertEquals(10.0, lista.get(0).cantidadSolicitada());
    mockServer.verify();
  }

  @Test
  void registrarNecesidad_Respuesta201_RetornaExito() {
    var form = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadForm();
    form.setSubcategoriaId(1L);
    form.setDescripcion("Leche para merendero");
    form.setCantidadSolicitada(20.0);
    form.setEsExtraordinaria(true);

    mockServer.expect(requestTo(baseUrl + "/entidades/15/necesidades"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));

    var res = entidadApiService.registrarNecesidad(15L, form);

    assertTrue(res.exito());
    mockServer.verify();
  }

  @Test
  void registrarNecesidad_Error403_RetornaFalloPermisos() {
    var form = new ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadForm();
    form.setSubcategoriaId(1L);
    form.setDescripcion("Leche");
    form.setCantidadSolicitada(5.0);

    mockServer.expect(requestTo(baseUrl + "/entidades/15/necesidades"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));

    var res = entidadApiService.registrarNecesidad(15L, form);

    assertFalse(res.exito());
    assertTrue(res.mensaje().contains("permisos"));
    mockServer.verify();
  }

  @Test
  void eliminarNecesidad_Respuesta204_RetornaExito() {
    mockServer.expect(requestTo(baseUrl + "/entidades/15/necesidades/99"))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withNoContent());

    var res = entidadApiService.eliminarNecesidad(15L, 99L);

    assertTrue(res.exito());
    mockServer.verify();
  }

  @Test
  void dashboard_Respuesta200_RetornaDTO() {
    String json = """
        {
          "razonSocial": "Comedor del Sol",
          "totalAsignadas": 3,
          "entregadas": 1,
          "entregasActivas": 1,
          "necesidadesActivas": 2,
          "donaciones": [
            {
              "id": 100,
              "descripcion": "Leche en polvo",
              "estado": "ASIGNADA",
              "fecha": "2026-10-01",
              "donanteNombre": "Maria",
              "donanteId": 5,
              "cantidadBienes": 10,
              "bienes": []
            }
          ]
        }
        """;

    mockServer.expect(requestTo(baseUrl + "/entidades/me/dashboard"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    var opt = entidadApiService.dashboard();

    assertTrue(opt.isPresent());
    assertEquals("Comedor del Sol", opt.get().razonSocial());
    assertEquals(1, opt.get().donaciones().size());
    mockServer.verify();
  }

  @Test
  void donacion_Respuesta200_RetornaDTO() {
    String json = """
        {
          "id": 200,
          "descripcion": "Fideos",
          "estado": "EN_TRASLADO",
          "fecha": "2026-10-02",
          "donanteNombre": "Carlos",
          "donanteId": 8,
          "entidadId": 15,
          "entidadNombre": "Hogar",
          "cantidadBienes": 5,
          "bienes": []
        }
        """;

    mockServer.expect(requestTo(baseUrl + "/donaciones-independientes/200"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    var opt = entidadApiService.donacion(200L);

    assertTrue(opt.isPresent());
    assertEquals(200L, opt.get().id());
    assertEquals("Carlos", opt.get().donanteNombre());
    assertEquals(15L, opt.get().entidadId());
    mockServer.verify();
  }

  @Test
  void entregas_Respuesta200ConCamion_RetornaLista() {
    String json = """
        [
          {
            "idEntrega": "uuid-123",
            "donacionId": 50,
            "estadoActual": "EN_TRASLADO",
            "patenteCamion": "AB123CD",
            "latitud": -34.6037,
            "longitud": -58.3816,
            "ultimaActualizacion": "2026-10-09T14:30:00"
          }
        ]
        """;

    mockServer.expect(requestTo("http://localhost:8083/entregas/por-donaciones?ids=50"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    var lista = entidadApiService.entregas(java.util.List.of(50L));

    assertEquals(1, lista.size());
    assertEquals("uuid-123", lista.get(0).idEntrega());
    assertEquals(50L, lista.get(0).donacionId());
    assertEquals("AB123CD", lista.get(0).patenteCamion());
    assertEquals(-34.6037, lista.get(0).latitud());
    mockServer.verify();
  }

  @Test
  void entregas_Respuesta200SinCamion_RetornaLista() {
    String json = """
        [
          {
            "idEntrega": "uuid-456",
            "donacionId": 60,
            "estadoActual": "LISTA",
            "patenteCamion": null,
            "latitud": null,
            "longitud": null,
            "ultimaActualizacion": null
          }
        ]
        """;

    mockServer.expect(requestTo("http://localhost:8083/entregas/por-donaciones?ids=60"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    var lista = entidadApiService.entregas(java.util.List.of(60L));

    assertEquals(1, lista.size());
    assertEquals("uuid-456", lista.get(0).idEntrega());
    assertNull(lista.get(0).patenteCamion());
    assertNull(lista.get(0).latitud());
    mockServer.verify();
  }

  @Test
  void entregas_ServicioCaido_RetornaListaVaciaSinExcepcion() {
    mockServer.expect(requestTo("http://localhost:8083/entregas/por-donaciones?ids=50"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    var lista = entidadApiService.entregas(java.util.List.of(50L));

    assertTrue(lista.isEmpty());
    mockServer.verify();
  }

  @Test
  void confirmarRecepcion_Respuesta200_RetornaOk() {
    mockServer.expect(requestTo(baseUrl + "/donaciones-independientes/100/recepcion"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.OK));

    var res = entidadApiService.confirmarRecepcion(100L, java.util.List.of("http://foto.jpg"), "Todo ok", true);

    assertTrue(res.exito());
    assertEquals("Confirmación de recepción registrada.", res.mensaje());
    mockServer.verify();
  }

  @Test
  void confirmarRecepcion_Respuesta403_RetornaErrorNoEsTuya() {
    mockServer.expect(requestTo(baseUrl + "/donaciones-independientes/100/recepcion"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));

    var res = entidadApiService.confirmarRecepcion(100L, java.util.List.of("http://foto.jpg"), null, true);

    assertFalse(res.exito());
    assertTrue(res.mensaje().contains("no es tuya"));
    mockServer.verify();
  }

  @Test
  void confirmarRecepcion_Respuesta400_RetornaError() {
    mockServer.expect(requestTo(baseUrl + "/donaciones-independientes/100/recepcion"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    var res = entidadApiService.confirmarRecepcion(100L, java.util.List.of("http://foto.jpg"), null, true);

    assertFalse(res.exito());
    assertTrue(res.mensaje().contains("inválidos"));
    mockServer.verify();
  }

  @Test
  void confirmarRecepcion_Respuesta502_RetornaErrorReintenta() {
    mockServer.expect(requestTo(baseUrl + "/donaciones-independientes/100/recepcion"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

    var res = entidadApiService.confirmarRecepcion(100L, java.util.List.of("http://foto.jpg"), null, true);

    assertFalse(res.exito());
    assertTrue(res.mensaje().contains("reintentá"));
    mockServer.verify();
  }
}


