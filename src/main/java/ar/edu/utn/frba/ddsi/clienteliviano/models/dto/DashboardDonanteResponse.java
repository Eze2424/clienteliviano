package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import java.util.List;
import lombok.Data;

@Data
public class DashboardDonanteResponse {
  private String nombre;
  private String apellido;
  private Integer totalDonaciones;
  private Integer ongsBeneficiadas;
  private Integer donacionesEntregadas;
  private List<DonacionResumenDTO> donacionesRecientes;
}
