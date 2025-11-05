package com.acl.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.acl.backend.data.AnalysisData.AnalysisResult;

/**
 * Servicio de análisis que ahora usa IA (Gemini) como metodo principal
 * y mantiene el análisis por regex como fallback
 */
@Service
public class NLPAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(NLPAnalysisService.class);

    private final AIAnalysisService aiAnalysisService;
    private final LegacyAnalysisService legacyAnalysisService;

    @Value("${analysis.use-ai:true}")
    private boolean useAI;

    public NLPAnalysisService(AIAnalysisService aiAnalysisService,
                              LegacyAnalysisService legacyAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
        this.legacyAnalysisService = legacyAnalysisService;
    }

    // Metodo principal de analisis - Usa IA o Fallback
    public AnalysisResult analyze(String text) {
        if (useAI) {
            try {
                log.info("Usando análisis con IA");
                return aiAnalysisService.analyzeContract(text);
            } catch (Exception e) {
                log.warn("Fallo en análisis con IA, usando método legacy: {}", e.getMessage());
                return legacyAnalysisService.analyzeWithRegex(text);
            }
        } else {
            log.info("Usando análisis legacy (regex)");
            return legacyAnalysisService.analyzeWithRegex(text);
        }
    }

    // Responde preguntas sobre el contrato
    public List<String> answerQuestions(String text, String question) {
        if(useAI){
            try {
                return aiAnalysisService.answerQuestion(text, question);
            } catch (Exception e) {
                log.warn("Fallo en Q&A con IA: {}", e.getMessage());
                return legacyAnalysisService.answerQuestionWithRegex(text, question);
            }
        } else {
            return legacyAnalysisService.answerQuestionWithRegex(text, question);
        }
    }

    // Genera resumen del contrato
    public String generateSummary(String text) {
        if (useAI) {
            try {
                return aiAnalysisService.generateSummary(text);
            } catch (Exception e) {
                log.warn("Fallo en resumen con IA: {}", e.getMessage());
                return "Resumen no disponible.";
            }
        }
        return "Resumen no disponible (IA deshabilitada";
    }

    // Detecta tipo de contrato
    public String detectType(String text) {
        if (useAI) {
            try {
                return aiAnalysisService.detectContractType(text);
            } catch (Exception e) {
                log.warn("Fallo en detección de tipo con IA: {}", e.getMessage());
                return legacyAnalysisService.detectTypeWithRegex(text);
            }
        } else {
            return legacyAnalysisService.detectTypeWithRegex(text);
        }
    }


}
