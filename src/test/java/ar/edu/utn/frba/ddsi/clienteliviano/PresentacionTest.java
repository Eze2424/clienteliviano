package ar.edu.utn.frba.ddsi.clienteliviano;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.UsuarioActual;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.MisionEnCursoResponse;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista;
import ar.edu.utn.frba.ddsi.clienteliviano.web.ProyeccionMapa;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Cubre la lógica de presentación que las plantillas invocan directamente.
 * No prueba reglas de negocio: esas viven en la API, no acá.
 */
class PresentacionTest {

  private DonacionVista conEstado(String estado, Long entidadId) {
    return new DonacionVista(1L, "Arroz", estado, LocalDate.now(), null, false,
        1L, "Elena", entidadId, entidadId == null ? null : "Comedor", 10, null);
  }

  @Test
  void traduceCadaEstadoDelEnumDelBackend() {
    assertEquals("En depósito", conEstado("EN_DEPOSITO", null).etiquetaEstado());
    assertEquals("En traslado", conEstado("EN_TRASLADO", 7L).etiquetaEstado());
    assertEquals("Entrega fallida", conEstado("ENTREGA_FALLIDA", 7L).etiquetaEstado());
    // Un estado que el backend agregue y el cliente no conozca se muestra crudo,
    // nunca en blanco: es preferible un texto feo a una celda vacía.
    assertEquals("ESTADO_NUEVO", conEstado("ESTADO_NUEVO", null).etiquetaEstado());
  }

  @Test
  void elBadgeAcompanaAlEstado() {
    assertEquals("badge-success", conEstado("ENTREGADA", 7L).claseBadge());
    assertEquals("badge-danger", conEstado("ENTREGA_FALLIDA", 7L).claseBadge());
    assertEquals("badge-neutral", conEstado("ESTADO_NUEVO", null).claseBadge());
  }

  @Test
  void sabeSiLaDonacionTieneDestino() {
    assertTrue(conEstado("ASIGNADA", 7L).asignada());
    assertFalse(conEstado("EN_DEPOSITO", null).asignada());
  }

  @Test
  void laProyeccionDelMapaQuedaDentroDelLienzo() {
    // La Plata y CABA caen en puntos distintos del recuadro...
    assertTrue(ProyeccionMapa.x(-57.9877) > ProyeccionMapa.x(-58.3816));
    assertTrue(ProyeccionMapa.y(-34.9611) > ProyeccionMapa.y(-34.6037));
    // ...y una coordenada fuera del recuadro se acota, no se sale del cuadro.
    for (double lon : new double[] {-180, -58.3, 0, 180}) {
      int x = ProyeccionMapa.x(lon);
      assertTrue(x >= 5 && x <= 95, "x fuera de rango: " + x);
    }
    for (double lat : new double[] {-90, -34.6, 0, 90}) {
      int y = ProyeccionMapa.y(lat);
      assertTrue(y >= 5 && y <= 95, "y fuera de rango: " + y);
    }
  }

  @Test
  void elPorcentajeDeLaMisionNoDivideporCero() {
    assertEquals(60, new MisionEnCursoResponse("m", 3, 5).porcentaje());
    assertEquals(0, new MisionEnCursoResponse("m", 3, 0).porcentaje());
  }

  @Test
  void lasInicialesSalenDelNombreCompleto() {
    assertEquals("EM", new UsuarioActual(1L, "Elena Martínez", "e@x.org", Rol.DONANTE).getIniciales());
    assertEquals("CH", new UsuarioActual(1L, "Comedor Los Hornos", "c@x.org", Rol.ENTIDAD).getIniciales());
    assertEquals("CA", new UsuarioActual(1L, "Carlos", "c@x.org", Rol.ADMIN).getIniciales());
  }
}
