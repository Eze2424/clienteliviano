package ar.edu.utn.frba.ddsi.clienteliviano.web;

/**
 * Ubica una coordenada geográfica dentro del lienzo esquemático del mapa,
 * como porcentaje de ancho y alto.
 *
 * Es cálculo de PRESENTACIÓN, no de dominio: no decide rutas, distancias ni
 * tiempos —eso es de logistica-service—; solo convierte lat/lon en la posición
 * del pin. Cuando se integre un mapa real (Leaflet, Google Maps) esto se borra
 * y el componente del mapa recibe las coordenadas crudas.
 */
public final class ProyeccionMapa {

  // Recuadro que cubre AMBA y La Plata, que es donde operan las entidades.
  private static final double LON_MIN = -58.70;
  private static final double LON_MAX = -57.85;
  private static final double LAT_MIN = -35.05;
  private static final double LAT_MAX = -34.40;

  private ProyeccionMapa() {
  }

  /** Porcentaje horizontal, acotado al 5–95 % para que el pin no quede cortado. */
  public static int x(double longitud) {
    return acotar((longitud - LON_MIN) / (LON_MAX - LON_MIN) * 100);
  }

  /** Porcentaje vertical. Se invierte: más latitud es más arriba en pantalla. */
  public static int y(double latitud) {
    return acotar((LAT_MAX - latitud) / (LAT_MAX - LAT_MIN) * 100);
  }

  private static int acotar(double porcentaje) {
    return (int) Math.round(Math.max(5, Math.min(95, porcentaje)));
  }
}
