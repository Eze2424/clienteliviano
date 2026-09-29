package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Resumen del dashboard del donante en una sola llamada.
 * DTO definido por Scuri23 y Eze2424 como contrato del endpoint agregado
 * GET /donaciones-service/donantes/me/dashboard.
 *
 * Resuelve de un saque las brechas G1 y G2: trae las donaciones del donante
 * logueado sin que el cliente tenga que averiguar antes su donanteId.
 * PENDIENTE: confirmar con el equipo de backend que ese endpoint exista o se
 * vaya a construir; hoy no esta en donaciones-service.
 */
@Data
public class DashboardDonanteResponse {
  private String nombre;
  private String apellido;
  private Integer totalDonaciones;
  private Integer ongsBeneficiadas;
  private Integer donacionesEntregadas;
  private List<DonacionResumenDTO> donacionesRecientes = new ArrayList<>();
}
