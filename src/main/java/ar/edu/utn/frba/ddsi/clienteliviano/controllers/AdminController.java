package ar.edu.utn.frba.ddsi.clienteliviano.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AdminController {

  @PostMapping
  public String importarDonantes(@RequestParam("csv") MultipartFile archivo) {
    //post a donaciones
    return "redirect:/admin/dashboard";
  }

  @PostMapping("/admin/donaciones/{donacionId}/confirmar")
  public String confirmarAsignacion(
      @PathVariable Long donacionId,
      @RequestParam("necesidadId") Long necesidadId) {
    //post a donaciones
    return "redirect:/admin/dashboard";
  }

  /* @GetMapping
  public String getRanking(Model model) {
    //get a incentivos
    return "redirect:/admin/dashboard/rankings";
  } */

}
