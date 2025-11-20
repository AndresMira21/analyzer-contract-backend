package com.acl.backend.service;

import org.springframework.stereotype.Service;

@Service
public class PromptService {

    /**
     * Marco legal para Colombia, resumido para optimizar tokens.
     */
    private final String legalFramework = """
        MARCO LEGAL COLOMBIANO PARA CLASIFICACIÓN:

        - Laborales → Código Sustantivo del Trabajo (CST), jurisprudencia laboral.
        - Arrendamiento → Código Civil, Código de Comercio (si es local comercial).
        - Salud → Ley 100 de 1993, Decreto 780/2016, Ministerio de Salud.
        - Compraventa → Código Civil, Cámara de Comercio (si es mercantil).
        - Contratos estatales → Ley 80/1993, Ley 1150/2007, Decreto 1082/2015.
        - Servicios profesionales → Código Civil y Código de Comercio.
        - Préstamos → Código Civil, reglas de mutuo.
        - Licencias → Propiedad intelectual, Decisión Andina 351 y Código Civil.
        - Sociedad → Código de Comercio (sociedades), Registro Mercantil.
        - Confidencialidad (NDA) → Régimen civil/comercial general.
        - Otros contratos civiles/comerciales → Código Civil + Código de Comercio.
        - Contratos financieros → Supervisados por la Superintendencia Financiera.
        - Contratos administrativos → Estatuto General de Contratación (Ley 80).
    """;

    /**
     * Prompt para análisis legal completo con base normativa.
     */
    public String buildAnalysisPrompt(String contractText) {
        return """
            Eres un abogado experto en contratación en Colombia. 
            Analiza el contrato considerando su TIPO y MARCO LEGAL aplicable según la legislación colombiana:

            %s

            CONTRATO:
            ```
            %s
            ```

            Responde SOLO con un JSON con esta estructura:

            {
              "type": "Tipo de contrato identificado",
              "legalBasis": [
                "Listado de normas colombianas aplicables al tipo de contrato"
              ],
              "keyClauses": [
                "Cláusulas más relevantes encontradas"
              ],
              "risks": [
                "Riesgos jurídicos específicos según la legislación colombiana"
              ],
              "riskScore": 0-100,
              "recommendations": [
                "Recomendaciones puntuales basadas en el marco legal"
              ],
              "summary": "Resumen ejecutivo del contrato"
            }

            Consideraciones:
            - Usa exclusivamente normas colombianas aplicables.
            - Identifica cláusulas ausentes que deberían existir según la ley.
            - Evalúa abuso de posición dominante, desequilibrio contractual y riesgos.
            - El análisis debe ser profesional, técnico y claro.
            - No incluyas texto fuera del JSON.

        """.formatted(legalFramework, contractText);
    }

    /**
     * Prompt de preguntas y respuestas sobre el contrato.
     */
    public String buildQuestionPrompt(String contractText, String question) {
        return """
            Eres un abogado consultor especializado en contratos colombianos.

            CONTRATO:
            ```
            %s
            ```

            PREGUNTA:
            "%s"

            Responde SOLO con JSON:

            {
              "answer": "Respuesta clara basada únicamente en el contrato",
              "references": [
                "Cláusulas textuales del contrato que respalden la respuesta"
              ],
              "legalContext": [
                "Normas colombianas aplicables a la pregunta (si corresponden)"
              ],
              "confidence": "high | medium | low"
            }

            Si el contrato no contiene la respuesta, indícalo explícitamente.
            """.formatted(contractText, question);
    }

    /**
     * Prompt para resumen ejecutivo profesional.
     */
    public String buildSummaryPrompt(String contractText) {
        return """
            Resume profesionalmente el contrato con enfoque colombiano.

            CONTRATO:
            ```
            %s
            ```

            Devuelve SOLO JSON:
            {
              "type": "Tipo de contrato",
              "legalBasis": ["Normas aplicables al tipo de contrato"],
              "summary": "Descripción ejecutiva clara",
              "keyPoints": ["Puntos clave"],
              "parties": ["Parte 1", "Parte 2"],
              "duration": "Vigencia o plazo identificado",
              "financialTerms": ["Obligaciones económicas"],
              "termination": ["Condiciones de terminación"]
            }
            """.formatted(contractText);
    }

    /**
     * Prompt mejorado de comparación entre contratos.
     */
    public String buildComparisonPrompt(String c1, String c2) {
        return """
            Eres un abogado especializado en contratación colombiana.

            CONTRATO 1:
            ```
            %s
            ```

            CONTRATO 2:
            ```
            %s
            ```

            Compara ambos y devuelve SOLO JSON:
            {
              "differences": [
                {
                  "aspect": "Cláusula o elemento",
                  "contract1": "Cómo lo maneja el contrato 1",
                  "contract2": "Cómo lo maneja el contrato 2",
                  "impact": "high | medium | low"
                }
              ],
              "whichIsBetter": "1 | 2 | depende",
              "reason": "Razón jurídica clara",
              "missingInContract1": ["Cláusulas faltantes"],
              "missingInContract2": ["Cláusulas faltantes"]
            }
            """.formatted(c1, c2);
    }

    /**
     * Prompt para detección de tipo contractual con marco legal colombiano.
     */
    public String buildTypeDetectionPrompt(String contractText) {
        return """
            Identifica el tipo exacto de contrato y su marco legal colombiano.

            %s

            CONTRATO:
            ```
            %s
            ```

            Devuelve SOLO JSON:
            {
              "type": "Tipo principal",
              "subtype": "Subtipo específico",
              "legalBasis": ["Normas aplicables"],
              "confidence": "0-100",
              "reasoning": "Explicación breve"
            }
            """.formatted(legalFramework, contractText);
    }

    /**
     * Prompt para extraer fechas del contrato.
     */
    public String buildDatesExtractionPrompt(String contractText) {
        return """
            Extrae fechas y plazos relevantes del contrato.

            CONTRATO:
            ```
            %s
            ```

            Devuelve SOLO JSON:
            {
              "dates": [
                {
                  "type": "inicio | terminación | pago | aviso | renovación",
                  "value": "Fecha o descripción",
                  "importance": "high | medium | low"
                }
              ],
              "criticalDeadlines": [
                "Fechas que requieren especial atención"
              ]
            }
            """.formatted(contractText);
    }
}
