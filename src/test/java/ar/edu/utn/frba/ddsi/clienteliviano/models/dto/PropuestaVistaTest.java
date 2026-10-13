package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PropuestaVistaTest {

  @Test
  void scoreFormateadoYPorcentaje_conValor_formateaCorrectamente() {
    PropuestaVista p = new PropuestaVista(
        1L, 10L, "Comedor", "Comedor Esperanza", "Alimentos", "Leche", 50.0, false, 0.92
    );

    assertNotNull(p.scoreFormateado());
    assertTrue(p.scoreFormateado().contains("0") && (p.scoreFormateado().contains(".92") || p.scoreFormateado().contains(",92")));
    assertEquals("92%", p.scorePorcentaje());
    assertEquals("Comedor Esperanza", p.nombreEntidad());
  }

  @Test
  void scoreFormateadoYPorcentaje_conScoreNulo_retornaNull() {
    PropuestaVista p = new PropuestaVista(
        1L, 10L, "Comedor", "Comedor Esperanza", "Alimentos", "Leche", 50.0, false, null
    );

    assertNull(p.scoreFormateado());
    assertNull(p.scorePorcentaje());
  }

  @Test
  void constructoresSobrecargados_mantienenCompatibilidad() {
    PropuestaVista p8 = new PropuestaVista(
        1L, 10L, "Comedor", "Comedor Esperanza", "Alimentos", "Leche", 50.0, true
    );
    assertNull(p8.score());
    assertTrue(p8.esExtraordinaria());

    PropuestaVista p6 = new PropuestaVista(
        2L, 20L, "Hogar", "Ropa", "Abrigos", 15.0
    );
    assertNull(p6.score());
    assertFalse(p6.esExtraordinaria());
    assertEquals("Hogar", p6.nombreEntidad());
  }

  @Test
  void asignacionResponse_hayCoincidenciasYTienePropuestas() {
    PropuestaVista p = new PropuestaVista(
        1L, 10L, "Comedor", "Comedor Esperanza", "Alimentos", "Leche", 50.0, false, 0.88
    );

    // Caso con intersección
    AsignacionResponse conInterseccion = new AsignacionResponse(100L, List.of(p), List.of(), List.of(), true);
    assertTrue(conInterseccion.hayCoincidencias());
    assertTrue(conInterseccion.tienePropuestas());

    // Caso sin intersección pero con semántico / subatendidos
    AsignacionResponse sinInterseccion = new AsignacionResponse(100L, List.of(), List.of(p), List.of(p), false);
    assertFalse(sinInterseccion.hayCoincidencias());
    assertTrue(sinInterseccion.tienePropuestas());

    // Caso vacío
    AsignacionResponse vacio = new AsignacionResponse(100L, List.of(), List.of(), List.of(), false);
    assertFalse(vacio.hayCoincidencias());
    assertFalse(vacio.tienePropuestas());
  }
}
