package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import lombok.Data;

/**
 * Una donacion dentro del resumen del dashboard del donante.
 * DTO definido por Scuri23 y Eze2424 como contrato del endpoint agregado
 * GET /donaciones-service/donantes/me/dashboard.
 *
 * Es la forma que DEVUELVE la API. La vista no lo usa directamente: se mapea a
 * DonacionVista, que agrega la traduccion de estados y la clase del badge.
 */
@Data
public class DonacionResumenDTO {
  private Long id;
  private String titulo;
  private String entidadNombre;
  private String fecha;
  private String estado;
  private String descripcionBreve;
  private String imagenUrl;
}
