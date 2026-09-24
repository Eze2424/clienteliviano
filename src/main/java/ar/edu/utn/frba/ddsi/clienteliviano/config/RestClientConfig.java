package ar.edu.utn.frba.ddsi.clienteliviano.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestClientConfig {

  @Bean
  public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate();

    ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest httpRequest = attributes.getRequest();
        HttpSession session = httpRequest.getSession(false);

        if (session != null && session.getAttribute("JWT_TOKEN") != null) {
          String token = (String) session.getAttribute("JWT_TOKEN");
          request.getHeaders().setBearerAuth(token);
        }
      }
      return execution.execute(request, body);
    };

    restTemplate.getInterceptors().add(interceptor);
    return restTemplate;
  }
}