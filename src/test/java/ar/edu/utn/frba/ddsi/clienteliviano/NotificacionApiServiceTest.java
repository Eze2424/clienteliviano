package ar.edu.utn.frba.ddsi.clienteliviano;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NotificacionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.services.NotificacionApiService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class NotificacionApiServiceTest {

  private RestTemplate restTemplate;
  private MockRestServiceServer mockServer;
  private NotificacionApiService notificacionApiService;
  private final String baseUrl = "http://localhost:8081/notificaciones";

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
    notificacionApiService = new NotificacionApiService(restTemplate);
    ReflectionTestUtils.setField(notificacionApiService, "notificacionesUrl", baseUrl);
  }

  @Test
  @DisplayName("listar_Respuesta200_RetornaListaDeNotificaciones")
  void listar_Respuesta200_RetornaListaDeNotificaciones() {
    String json = """
        [
          {
            "id": 1,
            "tipo": "ASIGNACION_ENTIDAD",
            "titulo": "Nueva donación asignada",
            "detalle": "Donación #100",
            "fecha": "2026-10-09T15:30:00",
            "leida": false
          }
        ]
        """;

    mockServer.expect(requestTo(baseUrl + "?email=contacto%40entidad.org&destinatarioId=5"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    List<NotificacionResponse> lista = notificacionApiService.listar("contacto@entidad.org", 5L);

    assertEquals(1, lista.size());
    assertEquals("ASIGNACION_ENTIDAD", lista.get(0).tipo());
    assertEquals("Nueva donación asignada", lista.get(0).titulo());
    assertFalse(lista.get(0).leida());
    mockServer.verify();
  }

  @Test
  @DisplayName("listar_Respuesta200Vacia_RetornaListaVacia")
  void listar_Respuesta200Vacia_RetornaListaVacia() {
    mockServer.expect(requestTo(baseUrl + "?email=contacto%40entidad.org"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    List<NotificacionResponse> lista = notificacionApiService.listar("contacto@entidad.org", null);

    assertTrue(lista.isEmpty());
    mockServer.verify();
  }

  @Test
  @DisplayName("listar_ServicioCaido_RetornaListaVaciaSinExcepcion")
  void listar_ServicioCaido_RetornaListaVaciaSinExcepcion() {
    mockServer.expect(requestTo(baseUrl + "?email=contacto%40entidad.org"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    List<NotificacionResponse> lista = notificacionApiService.listar("contacto@entidad.org", null);

    assertTrue(lista.isEmpty());
    mockServer.verify();
  }

  @Test
  @DisplayName("listar_EmailConCaracteresEspeciales_CodificaUrlCorrectamente")
  void listar_EmailConCaracteresEspeciales_CodificaUrlCorrectamente() {
    mockServer.expect(requestTo(baseUrl + "?email=ong%2Btest%40sub.dominio.org"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    List<NotificacionResponse> lista = notificacionApiService.listar("ong+test@sub.dominio.org", null);

    assertNotNull(lista);
    mockServer.verify();
  }

  @Test
  @DisplayName("marcarLeida_Respuesta204_RetornaTrue")
  void marcarLeida_Respuesta204_RetornaTrue() {
    mockServer.expect(requestTo(baseUrl + "/42/leida"))
        .andExpect(method(HttpMethod.PATCH))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));

    boolean ok = notificacionApiService.marcarLeida(42L);

    assertTrue(ok);
    mockServer.verify();
  }

  @Test
  @DisplayName("marcarLeida_Respuesta404_RetornaFalse")
  void marcarLeida_Respuesta404_RetornaFalse() {
    mockServer.expect(requestTo(baseUrl + "/999/leida"))
        .andExpect(method(HttpMethod.PATCH))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    boolean ok = notificacionApiService.marcarLeida(999L);

    assertFalse(ok);
    mockServer.verify();
  }
}
