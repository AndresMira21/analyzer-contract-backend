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

@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${app.cors.allowed-origins:http://localhost:3000,https://analyzer-contract-frontend-kohl.vercel.app}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

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

        // Métodos permitidos - IMPORTANTE: Incluir OPTIONS para preflight
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers permitidos - IMPORTANTE: Authorization para JWT
        config.setAllowedHeaders(Arrays.asList(
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));

        // Headers expuestos
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition",
                "X-Total-Count"
        ));

        // Permitir credenciales (importante para cookies/auth)
        config.setAllowCredentials(true);

        // Cache de preflight en segundos
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("=== CORS Configuration Complete ===");
        log.info("✓ CORS habilitado para todos los endpoints");
        log.info("✓ Métodos permitidos: GET, POST, PUT, PATCH, DELETE, OPTIONS");
        log.info("✓ Headers permitidos: Content-Type, Authorization, X-Requested-With, Accept, Origin");

        return source;
    }
}