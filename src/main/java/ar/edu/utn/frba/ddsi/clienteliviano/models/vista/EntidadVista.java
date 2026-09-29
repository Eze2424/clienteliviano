package ar.edu.utn.frba.ddsi.clienteliviano.models.vista;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.NecesidadResponse;
import java.util.List;

/**
 * Una entidad beneficiaria como la muestra el catálogo del donante.
 *
 * BRECHA: EntidadResponse del backend trae razón social, dirección, coordenadas
 * y teléfono. No trae tipo de entidad, nivel de urgencia ni foto, que son
 * justamente los tres filtros de la vista. Ver brecha G6.
 */
public record EntidadVista(
    Long id,
    String razonSocial,
    String direccion,
    String telefono,
    double latitud,
    double longitud,
    String tipo,
    boolean urgenciaExtraordinaria,
    String foto,
    String fotoAlt,
    List<NecesidadResponse> necesidades
) {
  public String etiquetaUrgencia() {
    return urgenciaExtraordinaria ? "Necesidades extraordinarias" : "Necesidades recurrentes";
  }

  public String claseUrgencia() {
    return urgenciaExtraordinaria ? "badge-warning" : "badge-neutral";
  }
}
