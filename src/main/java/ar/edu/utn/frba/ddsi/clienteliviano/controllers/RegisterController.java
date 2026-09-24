package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.DonanteCreateRequest;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.EntidadCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {

  private final RestTemplate restTemplate;

  @Value("${backend.api.url.donaciones}")
  private String backendApiUrl;

  @GetMapping("/donante")
  public String mostrarFormularioRegistro(Model model) {
    model.addAttribute("donante", new DonanteCreateRequest());
    return "registro-donante"; // vistas
  }
  @GetMapping("/entidad")
  public String mostrarFormularioEntidad(Model model) {

    model.addAttribute("entidad", new EntidadCreateRequest());
    return "registro-entidad";
  }

  @PostMapping("/donante")
  public String registrarDonante(@ModelAttribute("donante") DonanteCreateRequest donanteRequest, Model model) {
    try {
      // PASO 1: Registrar la identidad en Keycloak
      boolean keycloakCreado = registrarYAsignarRolEnKeycloak(donanteRequest.getEmail(), donanteRequest.getPassword(), "DONANTE",donanteRequest.getNombre(),donanteRequest.getApellido());

      if (!keycloakCreado) {
        model.addAttribute("error", "No se pudo crear el usuario en el sistema de autenticación.");
        return "registro-donante";
      }

      // PASO 2: Registrar en el Backend de Donaciones (MySQL)
      // Aseguramos que el email del DTO sea el mismo que se mandó a Keycloak[cite: 2]
      String url = backendApiUrl + "/donantes";
      ResponseEntity<Void> response = restTemplate.postForEntity(url, donanteRequest, Void.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        return "redirect:/login?registroExitoso=true";
      } else {
        model.addAttribute("error", "Error inesperado al intentar registrar el usuario en el backend.");
        return "registro-donante";
      }
    } catch (HttpClientErrorException e) {
      model.addAttribute("error", "Datos inválidos: " + e.getResponseBodyAsString());
      return "registro-donante";
    } catch (Exception e) {
      model.addAttribute("error", "No se pudo conectar con el servidor: " + e.getMessage());
      return "registro-donante";
    }
  }

  // Método auxiliar para crear el usuario en Keycloak y asignarle su rol
  private boolean registrarYAsignarRolEnKeycloak(String email, String password, String rolName, String nombre, String apellido) {
    try {
      // ==========================================
      // PASO 1: Obtener el Token de Administrador
      // ==========================================
      String tokenEndpoint = "http://localhost:8085/realms/master/protocol/openid-connect/token";

      org.springframework.http.HttpHeaders tokenHeaders = new org.springframework.http.HttpHeaders();
      tokenHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

      org.springframework.util.MultiValueMap<String, String> tokenBody = new org.springframework.util.LinkedMultiValueMap<>();
      tokenBody.add("client_id", "admin-cli");
      tokenBody.add("username", "admin");
      tokenBody.add("password", "admin");
      tokenBody.add("grant_type", "password");

      org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> tokenRequest =
          new org.springframework.http.HttpEntity<>(tokenBody, tokenHeaders);

      org.springframework.http.ResponseEntity<java.util.Map> tokenResponse =
          restTemplate.postForEntity(tokenEndpoint, tokenRequest, java.util.Map.class);

      String adminToken = (String) tokenResponse.getBody().get("access_token");

      // ==========================================
      // PASO 2: Crear el usuario en tu realm
      // ==========================================
      String usersEndpoint = "http://localhost:8085/admin/realms/DonaTrack/users";

      org.springframework.http.HttpHeaders userHeaders = new org.springframework.http.HttpHeaders();
      userHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
      userHeaders.setBearerAuth(adminToken);

      java.util.Map<String, Object> credential = new java.util.HashMap<>();
      credential.put("type", "password");
      credential.put("value", password);
      credential.put("temporary", false);

      java.util.Map<String, Object> userBody = new java.util.HashMap<>();
      userBody.put("username", email);
      userBody.put("email", email);
      userBody.put("firstName", nombre);
      userBody.put("lastName", apellido);
      userBody.put("enabled", true);
      userBody.put("credentials", java.util.List.of(credential));

      org.springframework.http.HttpEntity<java.util.Map<String, Object>> userRequest =
          new org.springframework.http.HttpEntity<>(userBody, userHeaders);

      org.springframework.http.ResponseEntity<Void> userResponse =
          restTemplate.postForEntity(usersEndpoint, userRequest, Void.class);

      if (!userResponse.getStatusCode().is2xxSuccessful()) {
        System.err.println("Error al crear usuario en Keycloak.");
        return false;
      }

      // ==========================================
      // PASO 3: Obtener el ID del Usuario Creado
      // ==========================================
      // Keycloak devuelve la URL del nuevo usuario en el header "Location" (ej: /users/123-abc)
      String locationHeader = userResponse.getHeaders().getFirst("Location");
      if (locationHeader == null) {
        return false;
      }
      String userId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);

      // ==========================================
      // PASO 4: Buscar el Rol en Keycloak
      // ==========================================
      String roleEndpoint = "http://localhost:8085/admin/realms/DonaTrack/roles/" + rolName;

      org.springframework.http.HttpHeaders getHeaders = new org.springframework.http.HttpHeaders();
      getHeaders.setBearerAuth(adminToken);
      org.springframework.http.HttpEntity<Void> getRequest = new org.springframework.http.HttpEntity<>(getHeaders);

      org.springframework.http.ResponseEntity<java.util.Map> roleResponse = restTemplate.exchange(
          roleEndpoint, org.springframework.http.HttpMethod.GET, getRequest, java.util.Map.class);

      java.util.Map<String, Object> roleData = roleResponse.getBody();

      // ==========================================
      // PASO 5: Mapear el Rol al Usuario
      // ==========================================
      String mappingEndpoint = "http://localhost:8085/admin/realms/DonaTrack/users/" + userId + "/role-mappings/realm";

      org.springframework.http.HttpHeaders mappingHeaders = new org.springframework.http.HttpHeaders();
      mappingHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
      mappingHeaders.setBearerAuth(adminToken);

      // La API espera un array con el objeto del rol
      org.springframework.http.HttpEntity<java.util.List<java.util.Map<String, Object>>> mappingRequest =
          new org.springframework.http.HttpEntity<>(java.util.List.of(roleData), mappingHeaders);

      org.springframework.http.ResponseEntity<Void> mappingResponse =
          restTemplate.postForEntity(mappingEndpoint, mappingRequest, Void.class);

      return mappingResponse.getStatusCode().is2xxSuccessful();

    } catch (Exception e) {
      System.err.println("Error al contactar la Admin API de Keycloak: " + e.getMessage());
      return false;
    }
  }

  @PostMapping("/entidad")
  public String registrarEntidad(@ModelAttribute("entidad") EntidadCreateRequest entidadRequest, Model model) {
    try {
      boolean keycloakCreado = registrarYAsignarRolEnKeycloak(entidadRequest.getEmail(), entidadRequest.getPassword(), "ENTIDAD",entidadRequest.getRazonSocial(),entidadRequest.getDireccion());

      if (!keycloakCreado) {
        model.addAttribute("error", "No se pudo crear el usuario en el sistema de autenticación.");
        return "registro-donante";
      }

      String url = backendApiUrl + "/entidades";

      ResponseEntity<Void> response = restTemplate.postForEntity(
          url,
          entidadRequest,
          Void.class
      );

      if (response.getStatusCode().is2xxSuccessful()) {
        return "redirect:/login?registroExitoso=true";
      } else {
        model.addAttribute("error", "Error inesperado al registrar la entidad.");
        return "registro-entidad";
      }

    } catch (HttpClientErrorException e) {
      model.addAttribute("error", "Datos inválidos: " + e.getResponseBodyAsString());
      return "registro-entidad";
    } catch (Exception e) {
      model.addAttribute("error", "Error de conexión con el servidor: " + e.getMessage());
      return "registro-entidad";
    }
  }
}