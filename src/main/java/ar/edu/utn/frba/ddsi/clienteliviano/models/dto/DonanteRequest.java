package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;


import ar.edu.utn.frba.ddsi.clienteliviano.models.entities.TipoOrganizacion;
import lombok.Data;
import java.util.List;


@Data
public class DonanteRequest {

  private String nombre;
  private String apellido;
  private int edad;
  private  String documento;
  private  String genero;
  private  String direccion;

  private  String email;
  private  String telefono;
  private String medioPredeterminado;
  private Boolean esJuridico;

  private String razonSocial;
  private String rubro;
  private List<Long> representantesIds;
  private TipoOrganizacion tipoOrganizacion;
}
