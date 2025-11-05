package com.acl.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.acl.backend.data.AnalysisData.AnalysisResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Servicio principal que orquesta el análisis de contratos usando IA
 */
@Service
public class AIAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AIAnalysisService.class);
    private static final int MAX_CONTRACT_LENGTH = 30000; // ~20 páginas

    private final GeminiService geminiService;
    private final PromptService promptService;
    private final Gson gson;

    public AIAnalysisService(GeminiService geminiService, PromptService promptService) {
        this.geminiService = geminiService;
        this.promptService = promptService;
        this.gson = new Gson();
    }

    /**
     * Analiza un contrato completo con IA
     */
    public AnalysisResult analyzeContract(String contractText) {
        try {
            log.info("Iniciando análisis de contrato ({} caracteres)", contractText.length());

            // Validar longitud
            if (contractText.length() > MAX_CONTRACT_LENGTH) {
                log.warn("Contrato muy largo, truncando...");
                contractText = contractText.substring(0, MAX_CONTRACT_LENGTH) +
                        "\n\n[NOTA: Contrato truncado por límite de tamaño]";
            }

            // Construir prompt y llamar a Gemini
            String prompt = promptService.buildAnalysisPrompt(contractText);
            String jsonResponse = geminiService.generateContent(prompt);

            jsonResponse = cleanJsonResponse(jsonResponse);

            // Parsear JSON a objeto
            AnalysisResult result = parseAnalysisResult(jsonResponse);

            log.info("Análisis completado exitosamente");
            return result;

        } catch (Exception e) {
            log.error("Error en análisis de contrato: {}", e.getMessage(), e);
            return createFallbackAnalysis(e.getMessage());
        }
    }

    /**
     * Responde preguntas sobre un contrato específico
     */
    public List<String> answerQuestion(String contractText, String question) {
        try {
            log.info("Respondiendo pregunta: {}", question);

            contractText = geminiService.truncateIfNeeded(contractText, MAX_CONTRACT_LENGTH);

            String prompt = promptService.buildQuestionPrompt(contractText, question);
            String jsonResponse = geminiService.generateContent(prompt);
            jsonResponse = cleanJsonResponse(jsonResponse);

            // Parsear respuesta
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

            List<String> answers = new ArrayList<>();
            answers.add(json.get("answer").getAsString());

            // Agregar referencias si existen
            if (json.has("references") && json.get("references").isJsonArray()) {
                json.getAsJsonArray("references").forEach(ref ->
                        answers.add("📄 Referencia: " + ref.getAsString())
                );
            }

            return answers;

        } catch (Exception e) {
            log.error("Error respondiendo pregunta: {}", e.getMessage());
            return List.of("Lo siento, no pude procesar tu pregunta. Error: " + e.getMessage());
        }
    }

    /**
     * Genera un resumen ejecutivo del contrato
     */
    public String generateSummary(String contractText) {
        try {
            contractText = geminiService.truncateIfNeeded(contractText, MAX_CONTRACT_LENGTH);

            String prompt = promptService.buildSummaryPrompt(contractText);
            String jsonResponse = geminiService.generateContent(prompt);
            jsonResponse = cleanJsonResponse(jsonResponse);

            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
            return json.get("summary").getAsString();

        } catch (Exception e) {
            log.error("Error generando resumen: {}", e.getMessage());
            return "No se pudo generar el resumen.";
        }
    }

    /**
     * Detecta el tipo de contrato
     */
    public String detectContractType(String contractText) {
        try {
            // Para tipo solo necesitamos el inicio del contrato
            String snippet = contractText.substring(0, Math.min(5000, contractText.length()));

            String prompt = promptService.buildTypeDetectionPrompt(snippet);
            String jsonResponse = geminiService.generateContent(prompt);
            jsonResponse = cleanJsonResponse(jsonResponse);

            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
            return json.get("type").getAsString();

        } catch (Exception e) {
            log.error("Error detectando tipo: {}", e.getMessage());
            return "General";
        }
    }

    /**
     * Compara dos contratos
     */
    public JsonObject compareContracts(String contract1, String contract2) {
        try {
            contract1 = geminiService.truncateIfNeeded(contract1, MAX_CONTRACT_LENGTH / 2);
            contract2 = geminiService.truncateIfNeeded(contract2, MAX_CONTRACT_LENGTH / 2);

            String prompt = promptService.buildComparisonPrompt(contract1, contract2);
            String jsonResponse = geminiService.generateContent(prompt);
            jsonResponse = cleanJsonResponse(jsonResponse);

            return JsonParser.parseString(jsonResponse).getAsJsonObject();

        } catch (Exception e) {
            log.error("Error comparando contratos: {}", e.getMessage());
            return new JsonObject();
        }
    }

    /**
     * Limpia la respuesta JSON que a veces viene con markdown
     */
    private String cleanJsonResponse(String response) {
        // Remover ```json y ``` si existen
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        }
        if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }

    /**
     * Parsea el JSON de análisis a objeto AnalysisResult
     */
    private AnalysisResult parseAnalysisResult(String jsonResponse) {
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

            AnalysisResult result = new AnalysisResult();
            result.setType(json.get("type").getAsString());
            result.setSummary(json.get("summary").getAsString());
            result.setRiskScore(json.get("riskScore").getAsDouble());

            // Parsear arrays
            List<String> clauses = new ArrayList<>();
            json.getAsJsonArray("keyClauses").forEach(e -> clauses.add(e.getAsString()));
            result.setKeyClauses(clauses);

            List<String> risks = new ArrayList<>();
            json.getAsJsonArray("risks").forEach(e -> risks.add(e.getAsString()));
            result.setRisks(risks);

            List<String> recommendations = new ArrayList<>();
            json.getAsJsonArray("recommendations").forEach(e -> recommendations.add(e.getAsString()));
            result.setRecommendations(recommendations);

            return result;

        } catch (Exception e) {
            log.error("Error parseando JSON: {}", e.getMessage());
            throw new RuntimeException("Error procesando respuesta de IA", e);
        }
    }

    /**
     * Crea un análisis de respaldo en caso de error
     */
    private AnalysisResult createFallbackAnalysis(String errorMessage) {
        AnalysisResult result = new AnalysisResult();
        result.setType("Error en análisis");
        result.setSummary("No se pudo completar el análisis automático. " + errorMessage);
        result.setRiskScore(50.0);
        result.setKeyClauses(List.of("No se pudieron detectar cláusulas"));
        result.setRisks(List.of("Error en el análisis. Revise el contrato manualmente."));
        result.setRecommendations(List.of(
                "Solicite revisión manual del contrato",
                "Verifique que el formato del documento sea correcto",
                "Intente nuevamente más tarde"
        ));
        return result;
    }

    // Responde preguntas legales generales (sin contrato específico)

    public String answerGeneralLegalQuestion(String question) {
        try {
            log.info("Respondiendo pregunta legal general: {}", question);

            String prompt = String.format("""
                Eres un asistente legal experto. Responde la siguiente pregunta legal de manera clara, 
                precisa y profesional.

                PREGUNTA:
                %s

                Proporciona una respuesta educativa y profesional. Si la pregunta requiere asesoría 
                legal específica para un caso particular, recomienda consultar con un abogado profesional.

                IMPORTANTE: Responde de forma directa en texto plano, SIN formato JSON.
                """, question);

            return geminiService.generateContent(prompt);

        } catch (Exception e) {
            log.error("Error respondiendo pregunta general: {}", e.getMessage());
            return "Lo siento, no pude procesar tu pregunta en este momento. Por favor, intenta de nuevo o consulta con un profesional legal.";
        }
    }
}