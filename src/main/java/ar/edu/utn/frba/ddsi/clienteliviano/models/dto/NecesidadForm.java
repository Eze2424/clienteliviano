package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import lombok.Data;

/**
 * Alta y edición de una necesidad material de la entidad beneficiaria.
 * Se traduce a NecesidadCreateRequest(subcategoriaId, entidadId, descripcion,
 * cantidadSolicitada, esExtraordinaria) de donaciones-service.
 *
 * BRECHA G7: la API pide `subcategoriaId` pero no expone ningún endpoint que
 * liste las subcategorías, así que el selector no tiene de dónde poblarse.
 * Hasta que exista, el campo viaja como texto y el controller lo deja marcado.
 */
@Data
public class NecesidadForm {

  private Long id;
  private String subcategoria;
  private String descripcion;
  private Double cantidadSolicitada;

  /** Extraordinaria = puntual y urgente. Recurrente = necesidad de todos los meses. */
  private boolean esExtraordinaria;
}
