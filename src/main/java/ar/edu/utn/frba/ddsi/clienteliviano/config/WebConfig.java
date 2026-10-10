package ar.edu.utn.frba.ddsi.clienteliviano.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Value("${storage.recepciones.dir:./uploads/recepciones}")
  private String storageDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path uploadPath = Paths.get(storageDir).toAbsolutePath().normalize();
    String uploadUri = uploadPath.toUri().toString();
    if (!uploadUri.endsWith("/")) {
      uploadUri += "/";
    }
    registry.addResourceHandler("/uploads/recepciones/**")
        .addResourceLocations(uploadUri);
  }
}
