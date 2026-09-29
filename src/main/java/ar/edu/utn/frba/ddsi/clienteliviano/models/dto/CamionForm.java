package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import lombok.Data;

/**
 * Alta y edición de un camión.
 * Espeja CamionCreateRequest / CamionUpdateRequest de logistica-service.
 */
@Data
public class CamionForm {

  private String id;
  private String patente;
  private Double volumen;
  private Double altura;
  private Double capacidadDeCarga;
  private Double latitud;
  private Double longitud;
}
