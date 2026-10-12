package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Espejo de donaciones-service: GET /donaciones-service/{id}/propuestas
 * Es el resultado de los algoritmos de selección. El cliente solo lo muestra:
 * la decisión de qué entidad gana la donación la toma la persona administradora.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AsignacionResponse(
    Long donacionId,
    List<PropuestaVista> interseccion,
    List<PropuestaVista> subatendidos,
    List<PropuestaVista> semantico,
    Boolean hayCoincidencias
) {
  public List<PropuestaVista> getInterseccionSegura() {
    return interseccion != null ? interseccion : List.of();
  }

  public List<PropuestaVista> getSubatendidosSegura() {
    return subatendidos != null ? subatendidos : List.of();
  }

  public List<PropuestaVista> getSemanticoSegura() {
    return semantico != null ? semantico : List.of();
  }

  public boolean tienePropuestas() {
    return (interseccion != null && !interseccion.isEmpty())
        || (subatendidos != null && !subatendidos.isEmpty())
        || (semantico != null && !semantico.isEmpty());
  }
}
