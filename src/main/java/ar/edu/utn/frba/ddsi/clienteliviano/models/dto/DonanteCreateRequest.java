package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;


import ar.edu.utn.frba.ddsi.clienteliviano.models.entities.TipoOrganizacion;
import lombok.Data;
import java.util.List;


@Data
public class DonanteCreateRequest {

  private String nombre;
  private String apellido;
  private int edad;
  private  String documento;
  private  String genero;
  private  String direccion;

  /**
   * Solo para dar de alta la identidad en Keycloak. El DonanteCreateRequest del
   * backend NO tiene este campo, asi que no debe viajar en el cuerpo que se le
   * manda: al armar el request de la API hay que excluirlo.
   */
  private String password;

  private  String email;
  private  String telefono;
  private String medioPredeterminado;
  private Boolean esJuridico;

  private String razonSocial;
  private String rubro;
  private List<Long> representantesIds;
  private TipoOrganizacion tipoOrganizacion;
}
