package ar.edu.utn.frba.ddsi.clienteliviano.config;

import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ObservabilidadConfig {

  @Bean
  static BeanPostProcessor restTemplateObservable(ObjectProvider<ObservationRegistry> observationRegistry) {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof RestTemplate restTemplate) {
          observationRegistry.ifAvailable(restTemplate::setObservationRegistry);
        }
        return bean;
      }
    };
  }

  @Bean
  InitializingBean openTelemetryAppenderInitializer(OpenTelemetry openTelemetry) {
    return () -> OpenTelemetryAppender.install(openTelemetry);
  }
}
