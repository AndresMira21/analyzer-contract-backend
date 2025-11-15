package com.acl.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        // Definir esquema de seguridad JWT
        final String securitySchemeName = "Bearer Authentication";
        
        return new OpenAPI()
                .info(new Info()
                        .title("Analizador de Contratos Legales API")
                        .version("1.0.0")
                        .description("""
                            API REST para análisis de contratos legales con IA.
                            
                            Funcionalidades principales:
                            - Autenticación JWT
                            - Análisis de contratos con IA (Gemini)
                            - Chat interactivo sobre contratos
                            - Dashboard y estadísticas
                            - Notificaciones
                            
                            Para usar la API:
                            1. Regístrate en /api/auth/register
                            2. Obtén tu token JWT
                            3. Usa el token en el botón "Authorize" arriba
                            """)
                        .contact(new Contact()
                                .name("Soporte")
                                .email("soporte@analizadorcontratos.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("https://analyzer-contract-backend-production.up.railway.app")
                                .description("Servidor de Producción (Railway)"),
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor de Desarrollo Local")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingresa tu token JWT (sin 'Bearer')")));
    }
}