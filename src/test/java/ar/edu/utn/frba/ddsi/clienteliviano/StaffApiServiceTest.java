package ar.edu.utn.frba.ddsi.clienteliviano;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.BienForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.CamionForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.CamionResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonacionIndependienteResumenResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonanteAltaForm;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonanteResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.PageResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.ResultadoOperacion;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.SubcategoriaResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.services.StaffApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class StaffApiServiceTest {

  private RestTemplate restTemplate;
  private MockRestServiceServer mockServer;
  private StaffApiService staffApiService;
  private final String logisticaUrl = "http://localhost:8083";

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
    staffApiService = new StaffApiService(restTemplate);
    ReflectionTestUtils.setField(staffApiService, "logisticaUrl", logisticaUrl);
    ReflectionTestUtils.setField(staffApiService, "donacionesUrl", "http://localhost:8080/donaciones-service");
    ReflectionTestUtils.setField(staffApiService, "incentivosUrl", "http://localhost:8082/incentivos-service");
  }

  @Test
  void camiones_200_retornaLista() {
    String json = """
        [
          {
            "id": "6543210fedcba9876543210f",
            "patente": "AB123CD",
            "volumen": 25.0,
            "altura": 3.0,
            "capacidadDeCarga": 5000.0,
            "latitud": -34.6037,
            "longitud": -58.3816,
            "ultimaActualizacion": "2026-10-11T12:00:00"
          }
        ]
        """;

    mockServer.expect(requestTo(logisticaUrl + "/camiones"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    List<CamionResponse> resultado = staffApiService.camiones();

    assertEquals(1, resultado.size());
    assertEquals("AB123CD", resultado.get(0).patente());
    assertEquals("6543210fedcba9876543210f", resultado.get(0).id());
    mockServer.verify();
  }

  @Test
  void camiones_500_retornaListaVaciaSinLanzarExcepcion() {
    mockServer.expect(requestTo(logisticaUrl + "/camiones"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    List<CamionResponse> resultado = staffApiService.camiones();

    assertTrue(resultado.isEmpty());
    mockServer.verify();
  }

  @Test
  void camionPorId_200_retornaCamion() {
    String id = "6543210fedcba9876543210f";
    String json = """
        {
          "id": "6543210fedcba9876543210f",
          "patente": "AB123CD",
          "volumen": 25.0,
          "altura": 3.0,
          "capacidadDeCarga": 5000.0,
          "latitud": -34.6037,
          "longitud": -58.3816
        }
        """;

    mockServer.expect(requestTo(logisticaUrl + "/camiones/" + id))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    Optional<CamionResponse> resultado = staffApiService.camionPorId(id);

    assertTrue(resultado.isPresent());
    assertEquals("AB123CD", resultado.get().patente());
    mockServer.verify();
  }

  @Test
  void camionPorId_404_retornaEmpty() {
    String id = "6543210fedcba9876543210f";

    mockServer.expect(requestTo(logisticaUrl + "/camiones/" + id))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    Optional<CamionResponse> resultado = staffApiService.camionPorId(id);

    assertTrue(resultado.isEmpty());
    mockServer.verify();
  }

  @Test
  void crearCamion_validacionPatenteVacia_retornaError() {
    CamionForm form = new CamionForm();
    form.setPatente("  ");
    form.setCapacidadDeCarga(5000.0);
    form.setVolumen(25.0);
    form.setAltura(3.0);

    ResultadoOperacion op = staffApiService.crearCamion(form);

    assertFalse(op.exito());
    assertTrue(op.mensaje().contains("patente"));
  }

  @Test
  void crearCamion_validacionCapacidadInvalida_retornaError() {
    CamionForm form = new CamionForm();
    form.setPatente("AB123CD");
    form.setCapacidadDeCarga(0.0);
    form.setVolumen(25.0);
    form.setAltura(3.0);

    ResultadoOperacion op = staffApiService.crearCamion(form);

    assertFalse(op.exito());
    assertTrue(op.mensaje().contains("capacidad"));
  }

  @Test
  void crearCamion_ok_retornaResultadoExitoso() {
    CamionForm form = new CamionForm();
    form.setPatente("AB123CD");
    form.setCapacidadDeCarga(5000.0);
    form.setVolumen(25.0);
    form.setAltura(3.0);

    String responseJson = """
        {
          "id": "6543210fedcba9876543210f",
          "patente": "AB123CD",
          "volumen": 25.0,
          "altura": 3.0,
          "capacidadDeCarga": 5000.0,
          "latitud": 0.0,
          "longitud": 0.0
        }
        """;

    mockServer.expect(requestTo(logisticaUrl + "/camiones"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(responseJson));

    ResultadoOperacion op = staffApiService.crearCamion(form);

    assertTrue(op.exito());
    assertTrue(op.mensaje().contains("AB123CD"));
    mockServer.verify();
  }

  @Test
  void crearCamion_409PatenteDuplicada_retornaMensajeBackend() {
    CamionForm form = new CamionForm();
    form.setPatente("AB123CD");
    form.setCapacidadDeCarga(5000.0);
    form.setVolumen(25.0);
    form.setAltura(3.0);

    String errorJson = """
        {"error": "Ya existe un camión registrado con la patente AB123CD"}
        """;

    mockServer.expect(requestTo(logisticaUrl + "/camiones"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(errorJson));

    ResultadoOperacion op = staffApiService.crearCamion(form);

    assertFalse(op.exito());
    assertEquals("Ya existe un camión registrado con la patente AB123CD", op.mensaje());
    mockServer.verify();
  }

  @Test
  void actualizarCamion_ok_retornaResultadoExitoso() {
    String id = "6543210fedcba9876543210f";
    CamionForm form = new CamionForm();
    form.setPatente("AB123CD");
    form.setCapacidadDeCarga(6000.0);
    form.setVolumen(30.0);
    form.setAltura(3.2);

    String responseJson = """
        {
          "id": "6543210fedcba9876543210f",
          "patente": "AB123CD",
          "volumen": 30.0,
          "altura": 3.2,
          "capacidadDeCarga": 6000.0,
          "latitud": 0.0,
          "longitud": 0.0
        }
        """;

    mockServer.expect(requestTo(logisticaUrl + "/camiones/" + id))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

    ResultadoOperacion op = staffApiService.actualizarCamion(id, form);

    assertTrue(op.exito());
    assertTrue(op.mensaje().contains("AB123CD"));
    mockServer.verify();
  }

  @Test
  void actualizarCamion_404_retornaMensajeError() {
    String id = "6543210fedcba9876543210f";
    CamionForm form = new CamionForm();
    form.setPatente("AB123CD");
    form.setCapacidadDeCarga(6000.0);
    form.setVolumen(30.0);
    form.setAltura(3.2);

    String errorJson = """
        {"error": "No se encontró camión con id 6543210fedcba9876543210f"}
        """;

    mockServer.expect(requestTo(logisticaUrl + "/camiones/" + id))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(errorJson));

    ResultadoOperacion op = staffApiService.actualizarCamion(id, form);

    assertFalse(op.exito());
    assertEquals("No se encontró camión con id 6543210fedcba9876543210f", op.mensaje());
    mockServer.verify();
  }

  @Test
  void eliminarCamion_ok_retornaExito() {
    String id = "6543210fedcba9876543210f";

    mockServer.expect(requestTo(logisticaUrl + "/camiones/" + id))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));

    ResultadoOperacion op = staffApiService.eliminarCamion(id);

    assertTrue(op.exito());
    mockServer.verify();
  }

  @Test
  void eliminarCamion_404_retornaMensajeError() {
    String id = "6543210fedcba9876543210f";

    mockServer.expect(requestTo(logisticaUrl + "/camiones/" + id))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    ResultadoOperacion op = staffApiService.eliminarCamion(id);

    assertFalse(op.exito());
    assertTrue(op.mensaje().contains("no fue encontrado"));
    mockServer.verify();
  }

  @Test
  void buscarDonantes_200_retornaPagina() {
    String json = """
        {
          "content": [
            {
              "donanteId": 1,
              "nombre": "Juan",
              "apellido": "Perez",
              "edad": 30,
              "dni": "35123456",
              "esJuridico": false,
              "email": "juan@test.com",
              "telefono": "11223344"
            }
          ],
          "page": 0,
          "size": 10,
          "totalElements": 1,
          "totalPages": 1
        }
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donantes?page=0&size=10&q=juan"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    PageResponse<DonanteResponse> res = staffApiService.buscarDonantes(0, 10, "juan");

    assertEquals(1, res.totalElements());
    assertEquals("Juan", res.content().get(0).nombre());
    mockServer.verify();
  }

  @Test
  void crearDonante_ok_retornaExito() {
    DonanteAltaForm form = new DonanteAltaForm();
    form.setNombre("Carlos");
    form.setApellido("Lopez");
    form.setEsJuridico(false);
    form.setDocumento("30123456");
    form.setEmail("carlos@test.com");
    form.setCrearUsuario(true);

    String responseJson = """
        {
          "donanteId": 2,
          "nombre": "Carlos",
          "apellido": "Lopez",
          "edad": 30,
          "dni": "30123456",
          "esJuridico": false,
          "email": "carlos@test.com"
        }
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donantes"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(responseJson));

    ResultadoOperacion op = staffApiService.crearDonante(form);

    assertTrue(op.exito());
    mockServer.verify();
  }

  @Test
  void crearDonante_409_retornaMensajeConflicto() {
    DonanteAltaForm form = new DonanteAltaForm();
    form.setNombre("Carlos");
    form.setApellido("Lopez");
    form.setEsJuridico(false);
    form.setDocumento("30123456");
    form.setEmail("carlos@test.com");

    String errorJson = """
        {"mensaje": "El email carlos@test.com ya está registrado para otro donante"}
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donantes"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(errorJson));

    ResultadoOperacion op = staffApiService.crearDonante(form);

    assertFalse(op.exito());
    assertEquals("El email carlos@test.com ya está registrado para otro donante", op.mensaje());
    mockServer.verify();
  }

  @Test
  void subcategorias_200_retornaLista() {
    String json = """
        [
          {"id": 1, "nombre": "Arroz", "categoriaId": 10},
          {"id": 2, "nombre": "Fideos", "categoriaId": 10}
        ]
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/subcategorias"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    List<SubcategoriaResponse> res = staffApiService.subcategorias();

    assertEquals(2, res.size());
    assertEquals("Arroz", res.get(0).nombre());
    mockServer.verify();
  }

  @Test
  void crearDonacion_ok_retornaExito() {
    DonacionForm form = new DonacionForm();
    form.setDonanteId(1L);
    BienForm bien = new BienForm();
    bien.setDescripcion("Leche larga vida");
    bien.setCantidad(10.0);
    bien.setSubcategoriaId(1L);
    form.setBienes(List.of(bien));

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donaciones"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{\"id\": 10}"));

    ResultadoOperacion op = staffApiService.crearDonacion(form);

    assertTrue(op.exito());
    mockServer.verify();
  }

  @Test
  void donacionesIndependientes_200_retornaPagina() {
    String json = """
        {
          "content": [
            {
              "id": 10,
              "descripcion": "Ropa variada",
              "subcategoria": "Ropa",
              "donanteId": 1,
              "donanteNombre": "Juan Perez",
              "estado": "EN_DEPOSITO",
              "vencida": false,
              "tienePropuestas": false
            }
          ],
          "page": 0,
          "size": 20,
          "totalElements": 1,
          "totalPages": 1
        }
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donaciones-independientes?page=0&size=20&estado=EN_DEPOSITO"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    PageResponse<DonacionIndependienteResumenResponse> res =
        staffApiService.donacionesIndependientes("EN_DEPOSITO", 0, 20);

    assertEquals(1, res.totalElements());
    assertEquals("Juan Perez", res.content().get(0).donanteNombre());
    mockServer.verify();
  }

  @Test
  void marcarDonacionVencida_ok_retornaExito() {
    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donaciones-independientes/10/estado?nuevoEstado=VENCIDA&descripcion=Vencimiento"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess());

    ResultadoOperacion op = staffApiService.marcarDonacionVencida(10L);

    assertTrue(op.exito());
    mockServer.verify();
  }

  @Test
  void marcarDonacionVencida_409_retornaMensajeError() {
    String errorJson = """
        {"mensaje": "La donación no puede marcarse como VENCIDA porque no tiene bienes vencidos"}
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donaciones-independientes/10/estado?nuevoEstado=VENCIDA&descripcion=Vencimiento"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(errorJson));

    ResultadoOperacion op = staffApiService.marcarDonacionVencida(10L);

    assertFalse(op.exito());
    assertEquals("La donación no puede marcarse como VENCIDA porque no tiene bienes vencidos", op.mensaje());
    mockServer.verify();
  }

  @Test
  void obtenerDonacionDetalle_200_retornaDonacion() {
    String json = """
        {
          "id": 10,
          "descripcion": "Leche en polvo",
          "estado": "EN_DEPOSITO",
          "fecha": "2026-10-10",
          "vencimiento": "2026-12-31",
          "donanteNombre": "Carlos Gomez",
          "donanteId": 1,
          "cantidadBienes": 5
        }
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donaciones-independientes/10"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    var opt = staffApiService.obtenerDonacionDetalle(10L);

    assertTrue(opt.isPresent());
    assertEquals(10L, opt.get().id());
    assertEquals("Leche en polvo", opt.get().descripcion());
    assertEquals("Carlos Gomez", opt.get().donante());
    mockServer.verify();
  }

  @Test
  void obtenerDonacionDetalle_404_retornaVacio() {
    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donaciones-independientes/999"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    var opt = staffApiService.obtenerDonacionDetalle(999L);

    assertTrue(opt.isEmpty());
    mockServer.verify();
  }

  @Test
  void obtenerPropuestas_200_retornaPropuestas() {
    String json = """
        {
          "donacionId": 10,
          "hayCoincidencias": true,
          "interseccion": [
            {
              "necesidadId": 101,
              "entidadId": 5,
              "entidad": "Comedor Los Hornos",
              "entidadNombre": "Comedor Los Hornos",
              "subcategoria": "Alimentos",
              "descripcion": "Leche",
              "cantidadSolicitada": 10.0,
              "esExtraordinaria": false
            }
          ],
          "subatendidos": [],
          "semantico": []
        }
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/10/propuestas"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    var opt = staffApiService.obtenerPropuestas(10L);

    assertTrue(opt.isPresent());
    assertEquals(10L, opt.get().donacionId());
    assertEquals(1, opt.get().interseccion().size());
    assertEquals(101L, opt.get().interseccion().get(0).necesidadId());
    mockServer.verify();
  }

  @Test
  void ejecutarAlgoritmos_200_retornaExito() {
    String json = """
        {
          "donacionId": 10,
          "hayCoincidencias": true,
          "interseccion": [],
          "subatendidos": [],
          "semantico": []
        }
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/10/ejecutar-algoritmos"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    ResultadoOperacion res = staffApiService.ejecutarAlgoritmos(10L);

    assertTrue(res.exito());
    mockServer.verify();
  }

  @Test
  void confirmarAsignacion_200_sinAdvertencia_retornaExito() {
    String json = """
        {
          "donacionId": 10,
          "entidadId": 5,
          "entidadNombre": "Comedor Los Hornos",
          "estado": "LISTA"
        }
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/10/confirmar-asignacion?necesidadId=101"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    ResultadoOperacion res = staffApiService.confirmarAsignacion(10L, 101L);

    assertTrue(res.exito());
    assertTrue(res.mensaje().contains("Comedor Los Hornos"));
    mockServer.verify();
  }

  @Test
  void confirmarAsignacion_200_conAdvertencia_retornaExitoConAdvertencia() {
    String json = """
        {
          "donacionId": 10,
          "entidadId": 5,
          "entidadNombre": "Comedor Los Hornos",
          "estado": "LISTA",
          "advertencia": "No se pudo registrar la Entrega en logistica: servicio no disponible"
        }
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/10/confirmar-asignacion?necesidadId=101"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    ResultadoOperacion res = staffApiService.confirmarAsignacion(10L, 101L);

    assertTrue(res.exito());
    assertTrue(res.mensaje().contains("aviso de logística"));
    mockServer.verify();
  }

  @Test
  void confirmarAsignacion_409_retornaMensajeError() {
    String errorJson = """
        {"error": "La necesidad indicada no pertenece a las propuestas calculadas para la donacion"}
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/10/confirmar-asignacion?necesidadId=999"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(errorJson));

    ResultadoOperacion res = staffApiService.confirmarAsignacion(10L, 999L);

    assertFalse(res.exito());
    assertEquals("La necesidad indicada no pertenece a las propuestas calculadas para la donacion", res.mensaje());
    mockServer.verify();
  }

  @Test
  void obtenerPeriodosRanking_200_retornaPeriodos() {
    String json = """
        ["2026-10", "2026-09"]
        """;

    mockServer.expect(requestTo("http://localhost:8082/incentivos-service/ranking/periodos"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    List<String> periodos = staffApiService.obtenerPeriodosRanking();

    assertEquals(2, periodos.size());
    assertEquals("2026-10", periodos.get(0));
    mockServer.verify();
  }

  @Test
  void obtenerRanking_200_retornaFilasMapeadas() {
    String json = """
        [
          {
            "posicion": 1,
            "idDonante": 10,
            "nombre": "Ana Gomez",
            "tipoDonante": "HUMANA",
            "categoria": "Comprometido",
            "misionesCumplidas": 8,
            "insignias": 3
          }
        ]
        """;

    mockServer.expect(requestTo("http://localhost:8082/incentivos-service/ranking?top=10&periodo=2026-10"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    var filas = staffApiService.obtenerRanking("2026-10", 10);

    assertEquals(1, filas.size());
    assertEquals(1, filas.get(0).posicion());
    assertEquals(10L, filas.get(0).donanteId());
    assertEquals("Ana Gomez", filas.get(0).nombre());
    assertEquals("Comprometido", filas.get(0).categoria());
    assertEquals(8L, filas.get(0).misionesCumplidas());
    assertEquals(3, filas.get(0).insignias());
    assertTrue(filas.get(0).esTop3());
    mockServer.verify();
  }

  @Test
  void obtenerRanking_500_retornaListaVacia() {
    mockServer.expect(requestTo("http://localhost:8082/incentivos-service/ranking?top=10&periodo=2026-10"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    var filas = staffApiService.obtenerRanking("2026-10", 10);

    assertTrue(filas.isEmpty());
    mockServer.verify();
  }

  @Test
  void iniciarImportacionCsv_202_retornaJobId() {
    String json = """
        { "jobId": "test-uuid-999" }
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donantes/importar"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(json));

    org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
        "archivo", "donantes.csv", "text/csv", "HUMANA|DNI|123|Juan|juan@test.com|1122".getBytes()
    );

    var res = staffApiService.iniciarImportacionCsv(file);

    assertTrue(res.exito());
    assertEquals("test-uuid-999", res.jobId());
    mockServer.verify();
  }

  @Test
  void iniciarImportacionCsv_archivoVacio_retornaError() {
    org.springframework.mock.web.MockMultipartFile emptyFile = new org.springframework.mock.web.MockMultipartFile(
        "archivo", "donantes.csv", "text/csv", new byte[0]
    );

    var res = staffApiService.iniciarImportacionCsv(emptyFile);

    assertFalse(res.exito());
    assertTrue(res.mensaje().contains("Elegí un archivo CSV"));
  }

  @Test
  void iniciarImportacionCsv_extensionInvalida_retornaError() {
    org.springframework.mock.web.MockMultipartFile txtFile = new org.springframework.mock.web.MockMultipartFile(
        "archivo", "donantes.txt", "text/plain", "datos".getBytes()
    );

    var res = staffApiService.iniciarImportacionCsv(txtFile);

    assertFalse(res.exito());
    assertTrue(res.mensaje().contains("extensión .csv"));
  }

  @Test
  void consultarEstadoImportacion_200_retornaJob() {
    String json = """
        {
          "jobId": "job-123",
          "estado": "FINALIZADO",
          "procesadas": 1000,
          "total": 1000,
          "creados": 800,
          "actualizados": 200,
          "errores": [],
          "mensaje": "Completado"
        }
        """;

    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donantes/importar/job-123"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    var jobOpt = staffApiService.consultarEstadoImportacion("job-123");

    assertTrue(jobOpt.isPresent());
    assertEquals("job-123", jobOpt.get().jobId());
    assertEquals("FINALIZADO", jobOpt.get().estado());
    assertEquals(800, jobOpt.get().creados());
    assertEquals(100, jobOpt.get().porcentaje());
    mockServer.verify();
  }

  @Test
  void consultarEstadoImportacion_404_retornaEmpty() {
    mockServer.expect(requestTo("http://localhost:8080/donaciones-service/donantes/importar/job-inexistente"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    var jobOpt = staffApiService.consultarEstadoImportacion("job-inexistente");

    assertTrue(jobOpt.isEmpty());
    mockServer.verify();
  }
}

