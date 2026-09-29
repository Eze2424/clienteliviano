package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import ar.edu.utn.frba.ddsi.clienteliviano.models.entities.TipoOrganizacion;
import lombok.Data;

/**
 * Objeto que respalda el formulario de alta de cuenta.
 *
 * Es la unión de los dos cuerpos que acepta la API —DonanteCreateRequest y
 * EntidadCreateRequest— más lo que necesita el alta de identidad. El controller
 * lo traduce al request que corresponda según `tipoCuenta`: el cliente no
 * decide reglas de negocio, solo elige a qué endpoint mandar.
 */
@Data
public class RegistroForm {

  /** PERSONA y ORGANIZACION van a POST /donantes; ENTIDAD va a POST /entidades. */
  private String tipoCuenta = "PERSONA";

  // --- Persona (o representante de la organización) ---
  private String nombre;
  private String apellido;
  private String documento;
  private Integer edad;
  private String genero;

  // --- Organización donante ---
  private String razonSocial;
  private TipoOrganizacion tipoOrganizacion;
  private String rubro;

  // --- Entidad beneficiaria ---
  private Double latitud;
  private Double longitud;

  // --- Contacto (común a los tres) ---
  private String direccion;
  private String email;
  private String telefono;
  private String medioPredeterminado = "EMAIL";

  // --- Cuenta ---
  private String password;
  private String passwordConfirmacion;
  private boolean aceptaTerminos;

  public boolean esEntidad() {
    return "ENTIDAD".equals(tipoCuenta);
  }

  public boolean esOrganizacion() {
    return "ORGANIZACION".equals(tipoCuenta);
  }
}
