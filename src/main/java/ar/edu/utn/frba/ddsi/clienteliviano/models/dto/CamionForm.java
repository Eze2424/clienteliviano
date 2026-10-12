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

  public static CamionForm desde(CamionResponse c) {
    CamionForm f = new CamionForm();
    f.setId(c.id());
    f.setPatente(c.patente());
    f.setVolumen(c.volumen());
    f.setAltura(c.altura());
    f.setCapacidadDeCarga(c.capacidadDeCarga());
    f.setLatitud(c.latitud());
    f.setLongitud(c.longitud());
    return f;
  }
}

