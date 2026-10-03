package ar.edu.utn.frba.ddsi.clienteliviano.services;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonanteCreateRequest;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.EntidadCreateRequest;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.RegistroForm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistroService {

  private final RestTemplate restTemplate;

  @Value("${backend.api.url.donaciones}")
  private String backendApiUrl;

  @Value("${keycloak.base-url}")
  private String keycloakBaseUrl;

  @Value("${keycloak.realm}")
  private String keycloakRealm;

  @Value("${keycloak.admin.realm}")
  private String adminRealm;

  @Value("${keycloak.admin.client-id}")
  private String adminClientId;

  @Value("${keycloak.admin.username}")
  private String adminUsername;

  @Value("${keycloak.admin.password}")
  private String adminPassword;

  public void procesarRegistro(RegistroForm form) {
    if (form.esEntidad()) {
      registrarEntidadDesdeForm(form);
    } else {
      registrarDonanteDesdeForm(form);
    }
  }

  public void registrarDonanteDesdeForm(RegistroForm form) {
    String nombre = form.esOrganizacion() ? form.getRazonSocial() : form.getNombre();
    String apellido = form.esOrganizacion() ? "" : (form.getApellido() != null ? form.getApellido() : "");

    // 1. Alta de usuario y rol en Keycloak
    boolean keycloakOk = registrarYAsignarRolEnKeycloak(form.getEmail(), form.getPassword(), "DONANTE", nombre, apellido);
    if (!keycloakOk) {
      throw new IllegalArgumentException("No se pudo crear el usuario en el sistema de autenticación (Keycloak).");
    }

    // 2. Alta en donaciones-service (MySQL)
    DonanteCreateRequest req = new DonanteCreateRequest();
    req.setEmail(form.getEmail());
    req.setTelefono(form.getTelefono());
    req.setMedioPredeterminado(form.getMedioPredeterminado() != null ? form.getMedioPredeterminado() : "EMAIL");
    req.setDireccion(form.getDireccion());

    if (form.esOrganizacion()) {
      req.setEsJuridico(true);
      req.setRazonSocial(form.getRazonSocial());
      req.setRubro(form.getRubro());
      req.setTipoOrganizacion(form.getTipoOrganizacion());
      req.setDocumento(form.getDocumento());
      req.setNombre(form.getRazonSocial());
      req.setApellido("");
      req.setEdad(0);
      req.setGenero("OTRO");
    } else {
      req.setEsJuridico(false);
      req.setNombre(form.getNombre());
      req.setApellido(form.getApellido());
      req.setEdad(form.getEdad() != null ? form.getEdad() : 18);
      req.setDocumento(form.getDocumento());
      req.setGenero(form.getGenero() != null ? form.getGenero() : "OTRO");
    }

    String url = backendApiUrl + "/donantes";
    restTemplate.postForEntity(url, req, Void.class);
  }

  public void registrarEntidadDesdeForm(RegistroForm form) {
    // 1. Alta de usuario y rol en Keycloak
    boolean keycloakOk = registrarYAsignarRolEnKeycloak(form.getEmail(), form.getPassword(), "ENTIDAD", form.getRazonSocial(), form.getDireccion());
    if (!keycloakOk) {
      throw new IllegalArgumentException("No se pudo crear el usuario en el sistema de autenticación (Keycloak).");
    }

    // 2. Alta en donaciones-service (MySQL)
    EntidadCreateRequest req = new EntidadCreateRequest();
    req.setRazonSocial(form.getRazonSocial());
    req.setDireccion(form.getDireccion());
    req.setTelefono(form.getTelefono());
    req.setLatitud(form.getLatitud() != null ? form.getLatitud() : -34.6037);
    req.setLongitud(form.getLongitud() != null ? form.getLongitud() : -58.3816);
    req.setEmail(form.getEmail());
    req.setMedioPredeterminado(form.getMedioPredeterminado() != null ? form.getMedioPredeterminado() : "EMAIL");

    String url = backendApiUrl + "/entidades";
    restTemplate.postForEntity(url, req, Void.class);
  }

  public boolean registrarYAsignarRolEnKeycloak(String email, String password, String rolName, String nombre, String apellido) {
    try {
      // 1. Obtener Token Admin
      String tokenEndpoint = keycloakBaseUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token";

      HttpHeaders tokenHeaders = new HttpHeaders();
      tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> tokenBody = new LinkedMultiValueMap<>();
      tokenBody.add("client_id", adminClientId);
      tokenBody.add("username", adminUsername);
      tokenBody.add("password", adminPassword);
      tokenBody.add("grant_type", "password");

      HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenBody, tokenHeaders);
      ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenEndpoint, tokenRequest, Map.class);

      String adminToken = (String) tokenResponse.getBody().get("access_token");

      // 2. Crear usuario en el realm
      String usersEndpoint = keycloakBaseUrl + "/admin/realms/" + keycloakRealm + "/users";

      HttpHeaders userHeaders = new HttpHeaders();
      userHeaders.setContentType(MediaType.APPLICATION_JSON);
      userHeaders.setBearerAuth(adminToken);

      Map<String, Object> credential = new HashMap<>();
      credential.put("type", "password");
      credential.put("value", password);
      credential.put("temporary", false);

      Map<String, Object> userBody = new HashMap<>();
      userBody.put("username", email);
      userBody.put("email", email);
      userBody.put("firstName", nombre != null ? nombre : "");
      userBody.put("lastName", apellido != null ? apellido : "");
      userBody.put("enabled", true);
      userBody.put("credentials", List.of(credential));

      HttpEntity<Map<String, Object>> userRequest = new HttpEntity<>(userBody, userHeaders);
      ResponseEntity<Void> userResponse = restTemplate.postForEntity(usersEndpoint, userRequest, Void.class);

      if (!userResponse.getStatusCode().is2xxSuccessful()) {
        return false;
      }

      // 3. Extraer ID del usuario creado desde Location
      String locationHeader = userResponse.getHeaders().getFirst("Location");
      if (locationHeader == null) {
        return false;
      }
      String userId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);

      // 4. Buscar rol en Keycloak
      String roleEndpoint = keycloakBaseUrl + "/admin/realms/" + keycloakRealm + "/roles/" + rolName;

      HttpHeaders getHeaders = new HttpHeaders();
      getHeaders.setBearerAuth(adminToken);
      HttpEntity<Void> getRequest = new HttpEntity<>(getHeaders);

      ResponseEntity<Map> roleResponse = restTemplate.exchange(roleEndpoint, HttpMethod.GET, getRequest, Map.class);
      Map<String, Object> roleData = roleResponse.getBody();

      // 5. Asignar rol
      String mappingEndpoint = keycloakBaseUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId + "/role-mappings/realm";

      HttpHeaders mappingHeaders = new HttpHeaders();
      mappingHeaders.setContentType(MediaType.APPLICATION_JSON);
      mappingHeaders.setBearerAuth(adminToken);

      HttpEntity<List<Map<String, Object>>> mappingRequest = new HttpEntity<>(List.of(roleData), mappingHeaders);
      ResponseEntity<Void> mappingResponse = restTemplate.postForEntity(mappingEndpoint, mappingRequest, Void.class);

      return mappingResponse.getStatusCode().is2xxSuccessful();
    } catch (HttpClientErrorException.Conflict e) {
      throw e;
    } catch (Exception e) {
      System.err.println("Error al contactar la Admin API de Keycloak: " + e.getMessage());
      return false;
    }
  }
}
