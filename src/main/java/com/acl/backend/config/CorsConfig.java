package com.acl.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://https://analyzer-contract-frontend-kohl.vercel.app}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Log para debugging
        log.info("=== CORS Configuration ===");
        log.info("Allowed origins from config: {}", allowedOrigins);

        // Parsear y agregar orígenes
        for (String origin : allowedOrigins.split(",")) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                if (trimmed.contains("*")) {
                    config.addAllowedOriginPattern(trimmed);
                    log.info("Added origin pattern: {}", trimmed);
                } else {
                    config.addAllowedOrigin(trimmed);
                    log.info("Added origin: {}", trimmed);
                }
            }
        }

        // Métodos permitidos
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers permitidos
        config.setAllowedHeaders(Arrays.asList("*"));

        // Headers expuestos
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));

        // Permitir credenciales
        config.setAllowCredentials(true);

        // Cache de preflight
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("=== CORS Configuration Complete ===");

        return source;
    }
}