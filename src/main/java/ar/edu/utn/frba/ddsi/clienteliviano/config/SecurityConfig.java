package ar.edu.utn.frba.ddsi.clienteliviano.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // Deshabilitamos CSRF temporalmente para facilitar la prueba
        .authorizeHttpRequests(auth -> auth
            // Permitimos el acceso libre a las vistas de registro y login de tu UI propia
            .requestMatchers("/login", "/register/**", "/css/**", "/js/**").permitAll()
            // El resto debe ser manejado por validación de sesión (opcionalmente con filtros manuales)
            .anyRequest().permitAll()
        );

    return http.build();
  }
}
