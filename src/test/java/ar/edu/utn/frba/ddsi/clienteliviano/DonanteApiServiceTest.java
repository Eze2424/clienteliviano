package ar.edu.utn.frba.ddsi.clienteliviano;

import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.RankingFila;
import ar.edu.utn.frba.ddsi.clienteliviano.services.DonanteApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class DonanteApiServiceTest {

  private RestTemplate restTemplate;
  private MockRestServiceServer mockServer;
  private DonanteApiService donanteApiService;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
    donanteApiService = new DonanteApiService(restTemplate);
    ReflectionTestUtils.setField(donanteApiService, "backendApiUrl", "http://localhost:8080/donaciones-service");
    ReflectionTestUtils.setField(donanteApiService, "incentivosUrl", "http://localhost:8082/incentivos-service");
    ReflectionTestUtils.setField(donanteApiService, "logisticaUrl", "http://localhost:8083");
  }

  @Test
  void obtenerPeriodosRanking_200_retornaListaPeriodos() {
    String json = """
        ["2026-10", "2026-09", "2026-08"]
        """;

    mockServer.expect(requestTo("http://localhost:8082/incentivos-service/ranking/periodos"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    List<String> periodos = donanteApiService.obtenerPeriodosRanking();

    assertEquals(3, periodos.size());
    assertEquals("2026-10", periodos.get(0));
    mockServer.verify();
  }

  @Test
  void obtenerPeriodosRanking_500_retornaFallbackPeriodoActual() {
    mockServer.expect(requestTo("http://localhost:8082/incentivos-service/ranking/periodos"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    List<String> periodos = donanteApiService.obtenerPeriodosRanking();

    assertNotNull(periodos);
    assertFalse(periodos.isEmpty());
    mockServer.verify();
  }

  @Test
  void obtenerRanking_200_retornaFilasMapeadas() {
    String json = """
        [
          {
            "posicion": 1,
            "idDonante": 10,
            "nombre": "Carlos Donante",
            "tipoDonante": "HUMANA",
            "categoria": "Colaborador",
            "misionesCumplidas": 5,
            "insignias": 2
          }
        ]
        """;

    mockServer.expect(requestTo("http://localhost:8082/incentivos-service/ranking?top=10&periodo=2026-10"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    List<RankingFila> filas = donanteApiService.obtenerRanking("2026-10", 10);

    assertEquals(1, filas.size());
    assertEquals(1, filas.get(0).posicion());
    assertEquals(10L, filas.get(0).donanteId());
    assertEquals(10L, filas.get(0).idDonante());
    assertEquals("Carlos Donante", filas.get(0).nombre());
    assertEquals("Colaborador", filas.get(0).categoria());
    assertEquals(5L, filas.get(0).misionesCumplidas());
    assertEquals(2, filas.get(0).insignias());
    assertEquals(600L, filas.get(0).puntajeImpacto()); // (5 * 100) + (2 * 50)
    assertTrue(filas.get(0).esTop3());
    mockServer.verify();
  }

  @Test
  void obtenerRanking_500_retornaListaVacia() {
    mockServer.expect(requestTo("http://localhost:8082/incentivos-service/ranking?top=10&periodo=2026-10"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    List<RankingFila> filas = donanteApiService.obtenerRanking("2026-10", 10);

    assertTrue(filas.isEmpty());
    mockServer.verify();
  }

  @Test
  void obtenerEntregaPorDonacion_200_retornaEntregaVistaMapeada() {
    String json = """
        [
          {
            "idEntrega": "ent-123",
            "donacionId": 42,
            "estadoActual": "EN_TRASLADO",
            "patenteCamion": "AA123BB",
            "latitud": -34.6037,
            "longitud": -58.3816,
            "ultimaActualizacion": "2026-10-21T18:00:00"
          }
        ]
        """;

    mockServer.expect(requestTo("http://localhost:8083/entregas/por-donaciones?ids=42"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    var entregaOpt = donanteApiService.obtenerEntregaPorDonacion(42L);

    assertTrue(entregaOpt.isPresent());
    var entrega = entregaOpt.get();
    assertEquals("ent-123", entrega.idEntrega());
    assertEquals(42L, entrega.donacionId());
    assertEquals("EN_TRASLADO", entrega.estado());
    assertEquals("AA123BB", entrega.patenteCamion());
    assertEquals(-34.6037, entrega.latitud(), 0.0001);
    assertEquals(-58.3816, entrega.longitud(), 0.0001);
    assertEquals("En camino", entrega.llegadaEstimada());
    mockServer.verify();
  }

  @Test
  void obtenerEntregaPorDonacion_500_retornaVacio() {
    mockServer.expect(requestTo("http://localhost:8083/entregas/por-donaciones?ids=42"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    var entregaOpt = donanteApiService.obtenerEntregaPorDonacion(42L);

    assertTrue(entregaOpt.isEmpty());
    mockServer.verify();
  }

  @Test
  void obtenerEntregaPorDonacion_nullId_retornaVacio() {
    var entregaOpt = donanteApiService.obtenerEntregaPorDonacion(null);
    assertTrue(entregaOpt.isEmpty());
  }

  @Test
  void obtenerEntregasActivasDonante_200_retornaEntregasMapeadas() {
    String dashboardJson = """
        {
          "nombre": "Carlos",
          "apellido": "Donante",
          "donacionesRecientes": [
            {
              "id": 42,
              "titulo": "Ropa de abrigo",
              "descripcionBreve": "Camperas y frazadas",
              "subdonaciones": [
                {
                  "id": 43,
                  "subcategoria": "Frazadas"
                }
              ]
            }
          ]
        }
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donantes/me/dashboard"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(dashboardJson, MediaType.APPLICATION_JSON));

    String entregasJson = """
        [
          {
            "idEntrega": "ent-101",
            "donacionId": 42,
            "estadoActual": "EN_TRASLADO",
            "patenteCamion": "AA123BB",
            "latitud": -34.6037,
            "longitud": -58.3816,
            "ultimaActualizacion": "2026-10-21T18:00:00"
          },
          {
            "idEntrega": "ent-102",
            "donacionId": 43,
            "estadoActual": "LISTA",
            "patenteCamion": "CC789DD",
            "latitud": -34.6100,
            "longitud": -58.3900,
            "ultimaActualizacion": "2026-10-21T18:30:00"
          }
        ]
        """;

    mockServer.expect(requestTo("http://localhost:8083/entregas/por-donaciones?ids=42,43"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(entregasJson, MediaType.APPLICATION_JSON));

    var entregas = donanteApiService.obtenerEntregasActivasDonante();

    assertEquals(2, entregas.size());
    assertEquals("ent-101", entregas.get(0).idEntrega());
    assertEquals(42L, entregas.get(0).donacionId());
    assertEquals("Camperas y frazadas", entregas.get(0).descripcionDonacion());
    assertEquals("AA123BB", entregas.get(0).patenteCamion());
    assertEquals("EN_TRASLADO", entregas.get(0).estado());

    assertEquals("ent-102", entregas.get(1).idEntrega());
    assertEquals(43L, entregas.get(1).donacionId());
    assertEquals("Frazadas", entregas.get(1).descripcionDonacion());
    assertEquals("CC789DD", entregas.get(1).patenteCamion());

    mockServer.verify();
  }

  @Test
  void obtenerEntregasActivasDonante_dashboardVacio_retornaListaVacia() {
    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donantes/me/dashboard"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    var entregas = donanteApiService.obtenerEntregasActivasDonante();

    assertTrue(entregas.isEmpty());
    mockServer.verify();
  }

  @Test
  void obtenerEntregasActivasDonante_logistica500_retornaListaVacia() {
    String dashboardJson = """
        {
          "nombre": "Carlos",
          "apellido": "Donante",
          "donacionesRecientes": [
            {
              "id": 42,
              "titulo": "Ropa de abrigo"
            }
          ]
        }
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donantes/me/dashboard"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(dashboardJson, MediaType.APPLICATION_JSON));

    mockServer.expect(requestTo("http://localhost:8083/entregas/por-donaciones?ids=42"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    var entregas = donanteApiService.obtenerEntregasActivasDonante();

    assertTrue(entregas.isEmpty());
    mockServer.verify();
  }
}
