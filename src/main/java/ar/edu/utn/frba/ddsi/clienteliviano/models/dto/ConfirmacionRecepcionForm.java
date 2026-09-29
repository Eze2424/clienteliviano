package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Confirmación de recepción de una donación por parte de la entidad
 * beneficiaria, con la evidencia fotográfica que pide el caso de uso.
 *
 * BRECHA: donaciones-service tiene el estado ENTREGADA en el enum TipoEstado
 * y logistica-service tiene PUT /entregas/{id}/resolucion, pero ninguno acepta
 * archivos. Falta definir con el equipo cómo se suben las fotos
 * (multipart al backend, o subida directa a un almacenamiento y envío de URLs).
 */
@Data
public class ConfirmacionRecepcionForm {

  private Long donacionId;

  /** Qué llegó y en qué condiciones. Queda en el registro de la entrega. */
  private String observaciones;

  /** Se recibió completo, o hubo faltantes o daños. */
  private boolean completa = true;

  private MultipartFile[] fotos;
}
