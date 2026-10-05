package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import ar.edu.utn.frba.ddsi.clienteliviano.models.Rol;
import ar.edu.utn.frba.ddsi.clienteliviano.models.Toast;
import ar.edu.utn.frba.ddsi.clienteliviano.models.dto.*;
import ar.edu.utn.frba.ddsi.clienteliviano.models.entities.TipoOrganizacion;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.DonacionVista;
import ar.edu.utn.frba.ddsi.clienteliviano.models.vista.RankingFila;
import ar.edu.utn.frba.ddsi.clienteliviano.web.Sesion;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/staff")
public class StaffController {

    private final Sesion sesion;
    private final RestTemplate restTemplate;

    @Value("${backend.api.url.donaciones}")
    private String donacionesUrl;

    @Value("${backend.api.url.logistica}")
    private String logisticaUrl;

    public StaffController(Sesion sesion, RestTemplate restTemplate) {
        this.sesion = sesion;
        this.restTemplate = restTemplate;
    }

    private boolean esAdmin(HttpSession session) {
        return sesion.tieneRol(session, Rol.ADMIN);
    }

    /* ---------- CU2 y CU3: depósito ---------- */

    // Estamos repitiendo la verificacion de sesion en cada controller. Supongo que se debe gestionar desde otro lado para que rechace directamente
    // en caso de que no sea admin y quiera entrar a /staff/*

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/login";

        List<DonacionVista> donaciones = new ArrayList<>();
        List<DonacionVista> pendientes = new ArrayList<>();
        List<CamionResponse> camiones = new ArrayList<>();

        try {
            // Le faltan datos al DTO que envia el back
            donaciones = restTemplate.exchange(
                    donacionesUrl + "/donaciones", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<DonacionVista>>() {
                    }).getBody();

            // Falta el endpoint para obtener las donaciones pendientes, supongo que seran las que estan en deposito?

            // pendientes = restTemplate.exchange(...)

            camiones = restTemplate.exchange(
                    logisticaUrl + "/camiones", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<CamionResponse>>() {
                    }).getBody();

        } catch (Exception e) {
            System.err.println("Error al cargar datos del dashboard: " + e.getMessage());
        }

        model.addAttribute("donaciones", donaciones != null ? donaciones : List.of());
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("camiones", camiones != null ? camiones : List.of());
        return "staff/dashboard";
    }

    @PostMapping("/donaciones/{id}/vencida")
    public String marcarVencida(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        if (!esAdmin(session)) return "redirect:/login";

        try {
            restTemplate.postForEntity(donacionesUrl + "/donaciones-independientes/" + id + "/estado?nuevoEstado=VENCIDA&descripcion=Vencimiento", null, Void.class);
            redirect.addFlashAttribute("toast", Toast.exito("Marcamos la donación #" + id + " como vencida. Sale del circuito de asignación."));
        } catch (Exception e) {
            redirect.addFlashAttribute("toast", Toast.error("No se pudo marcar como vencida: " + e.getMessage()));
        }

        return "redirect:/staff/dashboard";
    }

    @GetMapping("/donaciones/{id}/asignar")
    public String asignar(@PathVariable Long id, HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/login";

        try {
            DonacionVista donacion = restTemplate.getForObject(donacionesUrl + "/donaciones/" + id, DonacionVista.class);

            AsignacionResponse propuestas = restTemplate.getForObject(donacionesUrl + "/" + id + "/propuestas", AsignacionResponse.class);

            model.addAttribute("donacion", donacion);
            model.addAttribute("propuestas", propuestas);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Donación inexistente");
        }

        return "staff/asignar";
    }

    @PostMapping("/donaciones/{id}/asignar")
    public String confirmarAsignacion(@PathVariable Long id,
                                      @RequestParam Long necesidadId,
                                      @RequestParam String entidad,
                                      HttpSession session,
                                      RedirectAttributes redirect) {
        if (!esAdmin(session)) return "redirect:/login";

        try {
            String url = donacionesUrl + "/" + id + "/confirmar-asignacion?necesidadId=" + necesidadId + "&entidadId=" + entidad;
            restTemplate.postForEntity(url, null, Void.class);
            redirect.addFlashAttribute("toast", Toast.exito("Asignamos la donación #" + id + " a la entidad."));
        } catch (Exception e) {
            redirect.addFlashAttribute("toast", Toast.error("Fallo al asignar: " + e.getMessage()));
        }

        return "redirect:/staff/dashboard";
    }

    /* ---------- CU1: alta de donación ---------- */

    @GetMapping("/donaciones/nueva")
    public String nuevaDonacion(HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/login";

        try {
            List<DonanteResponse> donantes = restTemplate.exchange(
                    donacionesUrl + "/donantes", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<DonanteResponse>>() {
                    }).getBody();
            model.addAttribute("donantes", donantes);
        } catch (Exception e) {
            model.addAttribute("donantes", List.of());
        }

        model.addAttribute("form", new DonacionForm());
        return "staff/nueva-donacion";
    }

    @PostMapping("/donaciones/nueva")
    public String registrarDonacion(@ModelAttribute("form") DonacionForm form,
                                    HttpSession session, Model model,
                                    RedirectAttributes redirect) {
        if (!esAdmin(session)) return "redirect:/login";

        if (form.getDonanteId() == null) {
            try {
                model.addAttribute("donantes", restTemplate.exchange(donacionesUrl + "/donantes",
                        HttpMethod.GET, null, new ParameterizedTypeReference<List<DonanteResponse>>() {
                }).getBody());
            } catch (Exception ignored) {
            }

            model.addAttribute("error", "Elegí a quién pertenece la donación antes de registrarla.");
            return "staff/nueva-donacion";
        }

        try {
            restTemplate.postForEntity(donacionesUrl + "/donaciones", form, Void.class);
            redirect.addFlashAttribute("toast", Toast.exito("Registramos la donación. Ya entró al circuito de asignación."));
        } catch (Exception e) {
            redirect.addFlashAttribute("toast", Toast.error("Error al registrar donación: " + e.getMessage()));
        }

        return "redirect:/staff/dashboard";
    }

    /* ---------- CU1 y CU6: donantes e importación ---------- */

    @GetMapping("/donantes")
    public String donantes(HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/login";

        try {
            List<DonanteResponse> donantes = restTemplate.exchange(
                    donacionesUrl + "/donantes", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<DonanteResponse>>() {
                    }).getBody();
            model.addAttribute("donantes", donantes);
        } catch (Exception e) {
            model.addAttribute("donantes", List.of());
        }

        model.addAttribute("tiposOrganizacion", TipoOrganizacion.values());
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new DonanteAltaForm());
        }
        return "staff/donantes";
    }

    @PostMapping("/donantes")
    public String altaDonante(@ModelAttribute("form") DonanteAltaForm form,
                              HttpSession session, Model model,
                              RedirectAttributes redirect) {
        if (!esAdmin(session)) return "redirect:/login";

        if (form.getDocumento() == null || form.getDocumento().isBlank()) {
            model.addAttribute("error", "El número de documento es obligatorio para dar de alta un donante.");
            return donantes(session, model);
        }

        try {
            restTemplate.postForEntity(donacionesUrl + "/donantes", form, Void.class);
            redirect.addFlashAttribute("toast", Toast.exito("Dimos de alta al donante."));
        } catch (Exception e) {
            redirect.addFlashAttribute("toast", Toast.error("Error al registrar donante."));
        }

        return "redirect:/staff/donantes";
    }

    //Falta el endpoint en el back para importar el csv.

    @PostMapping("/donantes/importar")
    public String importarCsv(@RequestParam("archivo") MultipartFile archivo,
                              HttpSession session, RedirectAttributes redirect) {
        if (!esAdmin(session)) return "redirect:/login";

        if (archivo == null || archivo.isEmpty()) {
            redirect.addFlashAttribute("toast", Toast.error("Elegí un archivo CSV antes de importar."));
            return "redirect:/staff/donantes";
        }

        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.toLowerCase().endsWith(".csv")) {
            redirect.addFlashAttribute("toast", Toast.error("El archivo debe ser un CSV. Recibimos: " + nombre));
            return "redirect:/staff/donantes";
        }

        // El back todavia no tiene endpoint para importar el csv
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("archivo", archivo.getResource());
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(donacionesUrl + "/donantes/importar", requestEntity, Void.class);

        redirect.addFlashAttribute("toast", Toast.exito("CSV procesado cpm exito. Se importaron los datos del archivo."));
        return "redirect:/staff/donantes";
    }

    @GetMapping("/camiones")
    public String camiones(HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/login";

        try {
            List<CamionResponse> camiones = restTemplate.exchange(
                    logisticaUrl + "/camiones", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<CamionResponse>>() {
                    }).getBody();
            model.addAttribute("camiones", camiones);
        } catch (Exception e) {
            model.addAttribute("camiones", List.of());
        }

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CamionForm());
        }
        return "staff/camiones";
    }

    @PostMapping("/camiones")
    public String altaCamion(@ModelAttribute("form") CamionForm form,
                             HttpSession session, Model model,
                             RedirectAttributes redirect) {
        if (!esAdmin(session)) return "redirect:/login";

        if (form.getPatente() == null || form.getPatente().isBlank()) {
            model.addAttribute("error", "La patente es obligatoria.");
            return camiones(session, model);
        }

        try {
            restTemplate.postForEntity(logisticaUrl + "/camiones", form, Void.class);
            redirect.addFlashAttribute("toast", Toast.exito("Agregamos el camión " + form.getPatente() + " a la flota."));
        } catch (Exception e) {
            redirect.addFlashAttribute("toast", Toast.error("Error al registrar camión."));
        }

        return "redirect:/staff/camiones";
    }

    @PostMapping("/camiones/{id}/eliminar")
    public String bajaCamion(@PathVariable String id, HttpSession session,
                             RedirectAttributes redirect) {
        if (!esAdmin(session)) return "redirect:/login";

        try {
            restTemplate.delete(logisticaUrl + "/camiones/" + id);
            redirect.addFlashAttribute("toast", Toast.exito("Dimos de baja el camión."));
        } catch (Exception e) {
            redirect.addFlashAttribute("toast", Toast.error("Fallo al eliminar."));
        }

        return "redirect:/staff/camiones";
    }

    /* ---------- CU5: ranking ---------- */

    @GetMapping("/ranking")
    public String ranking(HttpSession session, Model model,
                          @RequestParam(required = false) String periodo) {
        if (!esAdmin(session)) return "redirect:/login";

        // Faltan los endpoints para los rankings en el back

        List<String> periodosDeRanking = List.of();
        List<RankingFila> ranking = List.of();
        String elegido = periodo != null ? periodo : "";

        model.addAttribute("ranking", ranking);
        model.addAttribute("periodos", periodosDeRanking);
        model.addAttribute("periodoActual", elegido);

        return "staff/ranking";
    }
}


