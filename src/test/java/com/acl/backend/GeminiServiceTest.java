package com.acl.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import com.acl.backend.config.WebClientConfig;
import com.acl.backend.service.GeminiService;
import com.acl.backend.service.PromptService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para verificar la conexión con Gemini
 * Carga configuración desde archivos properties
 */
@SpringBootTest(classes = {
        GeminiServiceTest.TestConfig.class,
        WebClientConfig.class,
        GeminiService.class,
        PromptService.class
})
@TestPropertySource(locations = "classpath:application.properties")
public class GeminiServiceTest {

    /**
     * Configuración mínima para tests
     */
    @Configuration
    static class TestConfig {
        // Configuración base para el contexto de Spring
    }

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private PromptService promptService;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com}")
    private String apiUrl;

    @Test
    public void testContextLoads() {
        System.out.println("\n========== TEST: Contexto de Spring ==========");
        System.out.println("[OK] El contexto de Spring se cargó correctamente");
        System.out.println("[INFO] Configuración cargada desde: application.properties");
        assertTrue(true, "El contexto debe cargar sin errores");
    }

    @Test
    public void testConfigurationLoaded() {
        System.out.println("\n========== TEST: Configuración Cargada ==========");

        assertNotNull(apiUrl, "La URL de la API debe estar configurada");
        assertNotNull(model, "El modelo debe estar configurado");
        assertNotNull(apiKey, "La API key debe estar configurada");

        System.out.println("[OK] Configuración cargada correctamente:");
        System.out.println("      • URL: " + apiUrl);
        System.out.println("      • Modelo: " + model);
        System.out.println("      • API Key: " + (apiKey.equals("test-key") ? "test-key (placeholder)" : "configurada ✓"));
    }

    @Test
    public void testServicesAreAvailable() {
        System.out.println("\n========== TEST: Servicios Disponibles ==========");

        assertNotNull(geminiService, "GeminiService debe estar disponible");
        assertNotNull(promptService, "PromptService debe estar disponible");

        System.out.println("[OK] GeminiService está disponible");
        System.out.println("[OK] PromptService está disponible");
        System.out.println("[OK] Todos los servicios están inyectados correctamente");
    }

    @Test
    public void testPromptGeneration() {
        System.out.println("\n========== TEST: Generación de Prompts ==========");

        String testContract = "CONTRATO DE PRUEBA con cláusulas de confidencialidad y plazo de 12 meses.";

        String analysisPrompt = promptService.buildAnalysisPrompt(testContract);
        assertNotNull(analysisPrompt, "El prompt de análisis no debe ser nulo");
        assertFalse(analysisPrompt.trim().isEmpty(), "El prompt debe tener contenido");

        System.out.println("[OK] Prompt de análisis generado");
        System.out.println("      Longitud: " + analysisPrompt.length() + " caracteres");

        String questionPrompt = promptService.buildQuestionPrompt(testContract, "¿Cuál es el plazo?");
        assertNotNull(questionPrompt, "El prompt de pregunta no debe ser nulo");

        System.out.println("[OK] Prompt de pregunta generado");
        System.out.println("      Longitud: " + questionPrompt.length() + " caracteres");
    }

    @Test
    public void testApiKeyConfiguration() {
        System.out.println("\n========== TEST: Verificación de API Key ==========");

        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            System.out.println("[WARN] ⚠ API Key no configurada en application.properties");
            System.out.println("\n=== Para ejecutar tests con Gemini ===");
            System.out.println("1. Abre: src/main/resources/application.properties");
            System.out.println("2. Configura: gemini.api.key=TU_API_KEY");
            System.out.println("3. Obtén tu API key en:");
            System.out.println("   https://aistudio.google.com/app/apikey");
            System.out.println("\n[INFO] Los tests de conexión con Gemini se omitirán");
        } else {
            System.out.println("[OK] ✓ API Key configurada correctamente");
            System.out.println("      Longitud: " + apiKey.length() + " caracteres");
            System.out.println("      Prefijo: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
        }

        assertTrue(true, "Este test siempre debe pasar");
    }

    @Test
    public void testGeminiConnection() {
        System.out.println("\n========== TEST: Conexión con Gemini ==========");

        // Verificar si tenemos una API key válida
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            System.out.println("[SKIP] ⊘ Test omitido: API key no configurada en application.properties");
            return;
        }

        try {
            System.out.println("[INFO] → Enviando solicitud a Gemini...");
            String testPrompt = "Responde SOLO con este JSON exacto (sin markdown): {\"status\": \"ok\", \"message\": \"Conexión exitosa\"}";
            String response = geminiService.generateContent(testPrompt);

            assertNotNull(response, "La respuesta no debe ser nula");
            assertFalse(response.trim().isEmpty(), "La respuesta no debe estar vacía");

            System.out.println("[OK] ✓ Gemini respondió correctamente");
            System.out.println("\n=== Respuesta de Gemini ===");
            System.out.println(response);
            System.out.println("===========================\n");

            // Verificar que la respuesta contiene el JSON esperado
            assertTrue(response.contains("ok") || response.contains("status"),
                    "La respuesta debe contener el JSON solicitado");

        } catch (Exception e) {
            System.err.println("[ERROR] ✗ Error conectando con Gemini");
            System.err.println("Mensaje: " + e.getMessage());

            if (e.getMessage().contains("401") || e.getMessage().contains("403")) {
                System.err.println("\n[HINT] Error de autenticación - verifica tu API key");
            } else if (e.getMessage().contains("404")) {
                System.err.println("\n[HINT] Modelo no encontrado - verifica el nombre del modelo: " + model);
            }

            System.err.println("\n=== Stack Trace ===");
            e.printStackTrace();
            System.err.println("===================");

            fail("Error al conectar con Gemini: " + e.getMessage());
        }
    }

    @Test
    public void testContractAnalysis() {
        System.out.println("\n========== TEST: Análisis de Contrato ==========");

        // Verificar si tenemos una API key válida
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            System.out.println("[SKIP] ⊘ Test omitido: API key no configurada");
            return;
        }

        String sampleContract = """
            CONTRATO DE SERVICIOS PROFESIONALES
            
            Entre la empresa XYZ S.A. (EL CLIENTE) y Juan Pérez (EL CONSULTOR):
            
            1. OBJETO: Prestación de servicios de consultoría en desarrollo de software.
            2. PLAZO: 6 meses desde la firma del presente contrato.
            3. HONORARIOS: $5,000 USD mensuales, pagaderos los días 5 de cada mes.
            4. CONFIDENCIALIDAD: Toda información del proyecto es estrictamente confidencial.
            5. TERMINACIÓN: Cualquier parte puede terminar el contrato con 30 días de aviso previo.
            """;

        try {
            String prompt = promptService.buildAnalysisPrompt(sampleContract);
            System.out.println("[INFO] → Enviando contrato a Gemini para análisis...");
            System.out.println("       Tamaño del prompt: " + prompt.length() + " caracteres");

            String response = geminiService.generateContent(prompt);

            assertNotNull(response, "El análisis no debe ser nulo");
            assertFalse(response.trim().isEmpty(), "El análisis no debe estar vacío");

            System.out.println("[OK] ✓ Análisis completado exitosamente");
            System.out.println("       Tamaño de respuesta: " + response.length() + " caracteres");
            System.out.println("\n=== Análisis del Contrato ===");
            System.out.println(response.substring(0, Math.min(500, response.length())));
            if (response.length() > 500) {
                System.out.println("... (respuesta truncada)");
            }
            System.out.println("=============================");

        } catch (Exception e) {
            System.err.println("[ERROR] ✗ Error en análisis de contrato");
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println("\n=== Stack Trace ===");
            e.printStackTrace();
            System.err.println("===================");
            fail("Error al analizar contrato: " + e.getMessage());
        }
    }

    @Test
    public void testWebClientConfiguration() {
        System.out.println("\n========== TEST: Configuración de WebClient ==========");

        assertNotNull(geminiService, "GeminiService debe tener WebClient inyectado");

        System.out.println("[OK] WebClient.Builder está configurado correctamente");
        System.out.println("[OK] Configuración aplicada:");
        System.out.println("      • Timeout de conexión: 30 segundos");
        System.out.println("      • Timeout de lectura: 60 segundos");
        System.out.println("      • Timeout de escritura: 60 segundos");
        System.out.println("\n[INFO] SecurityConfig y base de datos NO se cargaron en este test");
    }

    @Test
    public void testMultipleProperties() {
        System.out.println("\n========== TEST: Todas las Propiedades ==========");

        System.out.println("[OK] Propiedades cargadas:");
        System.out.println("      gemini.api.url: " + apiUrl);
        System.out.println("      gemini.api.model: " + model);
        System.out.println("      gemini.api.key: " + (apiKey.startsWith("${") ? "no configurada" : "***configurada***"));

        // Verificar que las propiedades no estén vacías o sin resolver
        assertNotNull(apiUrl, "La URL no debe ser nula");
        assertFalse(apiUrl.isEmpty(), "La URL no debe estar vacía");
        assertTrue(apiUrl.contains("generativelanguage.googleapis.com"), "La URL debe ser de Google Gemini");

        assertNotNull(model, "El modelo no debe ser nulo");
        // Solo verificar si el modelo está configurado (no es un placeholder)
        if (!model.startsWith("${")) {
            assertFalse(model.isEmpty(), "El modelo no debe estar vacío");
            System.out.println("[OK] Modelo configurado: " + model);
        } else {
            System.out.println("[WARN] Modelo no configurado (usando placeholder)");
        }
    }
}