package com.muni.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI documentation configuration.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortening Service REST API")
                        .version("1.0")
                        .description("A high-performance, production-ready URL Shortener using Spring Boot, MySQL, and Redis caching. "
                                     + "Features include analytics tracking, custom alias support, URL expiration, QR code generation, and rate limiting.")
                        .contact(new Contact()
                                .name("Muni")
                                .email("muni@example.com")));
    }
}
