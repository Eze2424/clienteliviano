package ar.edu.utn.frba.ddsi.clienteliviano.models;

/**
 * Roles del realm DonaTrack de Keycloak, tal como los lee el backend
 * (realm_access.roles -> ROLE_*). No inventamos roles: estos son los tres
 * que usan los @PreAuthorize de donaciones-service, incentivos-service
 * y logistica-service.
 */
public enum Rol {
  DONANTE("Donante", "/donante/dashboard"),
  ENTIDAD("Entidad beneficiaria", "/entidad/dashboard"),
  ADMIN("Administración", "/staff/dashboard");

  private final String etiqueta;
  private final String inicio;

  Rol(String etiqueta, String inicio) {
    this.etiqueta = etiqueta;
    this.inicio = inicio;
  }

  public String getEtiqueta() {
    return etiqueta;
  }

  /** Destino tras iniciar sesión. Cada rol entra a su propia área. */
  public String getInicio() {
    return inicio;
  }
}
