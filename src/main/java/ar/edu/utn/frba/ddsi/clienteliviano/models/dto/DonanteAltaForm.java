package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import ar.edu.utn.frba.ddsi.clienteliviano.models.entities.TipoOrganizacion;
import lombok.Data;

/**
 * Alta de un donante hecha por la persona administradora.
 * Espeja DonanteCreateRequest de donaciones-service.
 */
@Data
public class DonanteAltaForm {

  private String nombre;
  private String apellido;
  private Integer edad;
  private String documento;
  private String genero;
  private String direccion;
  private String email;
  private String telefono;
  private String medioPredeterminado = "EMAIL";
  private Boolean esJuridico = false;
  private String razonSocial;
  private String rubro;
  private TipoOrganizacion tipoOrganizacion;
  private Boolean crearUsuario = false;
}
