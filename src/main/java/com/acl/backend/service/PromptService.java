package com.acl.backend.service;

import org.springframework.stereotype.Service;


@Service
public class PromptService {

    /**
     * Prompt para análisis completo del contrato
     */
    public String buildAnalysisPrompt(String contractText) {
        return """
            Eres un experto analista legal especializado en revisión de contratos.
            Analiza el siguiente contrato y proporciona un análisis EXHAUSTIVO en formato JSON.
            
            CONTRATO:
            ```
            %s
            ```
            
            Debes responder ÚNICAMENTE con un objeto JSON con esta estructura exacta:
            {
              "type": "Tipo de contrato (ej: Laboral, Arrendamiento, Servicios, NDA, Compraventa, etc.)",
              "keyClauses": [
                "Lista de las cláusulas más importantes identificadas en el contrato"
              ],
              "risks": [
                "Lista detallada de riesgos potenciales, cláusulas abusivas, o aspectos problemáticos"
              ],
              "riskScore": 85.5,
              "recommendations": [
                "Recomendaciones específicas para mejorar el contrato o proteger al usuario"
              ],
              "summary": "Resumen ejecutivo del contrato en 2-3 oraciones"
            }
            
            CRITERIOS DE EVALUACIÓN:
            - riskScore: 0-100 (100 = sin riesgos, 0 = muy riesgoso)
            - Detecta: cláusulas abusivas, penalidades excesivas, falta de límites de responsabilidad
            - Identifica: obligaciones poco claras, prórrogas automáticas, renuncias a derechos
            - Verifica: presencia de cláusulas esenciales (terminación, confidencialidad, jurisdicción)
            
            RESPONDE SOLO CON EL JSON, SIN TEXTO ADICIONAL.
            """.formatted(contractText);
    }

    /**
     * Prompt para responder preguntas sobre el contrato (Q&A)
     */
    public String buildQuestionPrompt(String contractText, String question) {
        return """
            Eres un asistente legal experto. Tienes acceso al siguiente contrato:
            
            CONTRATO:
            ```
            %s
            ```
            
            PREGUNTA DEL USUARIO:
            %s
            
            Responde la pregunta de manera clara, precisa y basándote ÚNICAMENTE en el contenido del contrato.
            Si la información no está en el contrato, indícalo claramente.
            
            Proporciona tu respuesta en formato JSON:
            {
              "answer": "Respuesta detallada a la pregunta",
              "references": [
                "Citas textuales relevantes del contrato que respaldan tu respuesta"
              ],
              "confidence": "high/medium/low"
            }
            
            RESPONDE SOLO CON EL JSON, SIN TEXTO ADICIONAL.
            """.formatted(contractText, question);
    }

    /**
     * Prompt para generar un resumen ejecutivo
     */
    public String buildSummaryPrompt(String contractText) {
        return """
            Resume el siguiente contrato de manera ejecutiva y profesional.
            
            CONTRATO:
            ```
            %s
            ```
            
            Proporciona:
            1. Tipo de contrato
            2. Partes involucradas
            3. Objeto principal
            4. Plazo/vigencia
            5. Obligaciones principales de cada parte
            6. Aspectos financieros (si aplica)
            7. Condiciones de terminación
            
            Responde en formato JSON:
            {
              "summary": "Resumen ejecutivo completo",
              "keyPoints": [
                "Puntos clave del contrato"
              ],
              "parties": ["Parte 1", "Parte 2"],
              "duration": "Duración o vigencia",
              "mainObligations": {
                "parte1": ["obligación 1", "obligación 2"],
                "parte2": ["obligación 1", "obligación 2"]
              }
            }
            
            RESPONDE SOLO CON EL JSON, SIN TEXTO ADICIONAL.
            """.formatted(contractText);
    }

    /**
     * Prompt para comparar dos contratos
     */
    public String buildComparisonPrompt(String contract1, String contract2) {
        return """
            Eres un analista legal experto. Compara estos dos contratos y encuentra diferencias clave.
            
            CONTRATO 1:
            ```
            %s
            ```
            
            CONTRATO 2:
            ```
            %s
            ```
            
            Analiza y compara:
            - Cláusulas presentes en uno pero no en otro
            - Diferencias en términos y condiciones
            - Cuál contrato es más favorable y por qué
            - Riesgos relativos de cada uno
            
            Responde en formato JSON:
            {
              "differences": [
                {
                  "aspect": "Nombre del aspecto",
                  "contract1": "Qué dice el contrato 1",
                  "contract2": "Qué dice el contrato 2",
                  "impact": "high/medium/low"
                }
              ],
              "recommendation": "Cuál contrato es mejor y por qué",
              "missingInContract1": ["Cláusulas que faltan"],
              "missingInContract2": ["Cláusulas que faltan"]
            }
            
            RESPONDE SOLO CON EL JSON, SIN TEXTO ADICIONAL.
            """.formatted(contract1, contract2);
    }

    /**
     * Prompt para detectar el tipo de contrato
     */
    public String buildTypeDetectionPrompt(String contractText) {
        return """
            Identifica el tipo específico de este contrato.
            
            CONTRATO:
            ```
            %s
            ```
            
            Clasifícalo en una de estas categorías:
            - Laboral
            - Arrendamiento
            - Servicios Profesionales
            - Confidencialidad (NDA)
            - Compraventa
            - Préstamo
            - Sociedad
            - Licencia
            - Franquicia
            - Otro (especificar)
            
            Responde en formato JSON:
            {
              "type": "Tipo principal del contrato",
              "subtype": "Subtipo o clasificación más específica",
              "confidence": "Porcentaje de confianza (0-100)",
              "reasoning": "Breve explicación de por qué clasificaste así"
            }
            
            RESPONDE SOLO CON EL JSON, SIN TEXTO ADICIONAL.
            """.formatted(contractText);
    }

    /**
     * Prompt para extraer fechas y plazos importantes
     */
    public String buildDatesExtractionPrompt(String contractText) {
        return """
            Extrae todas las fechas, plazos y términos temporales importantes del contrato.
            
            CONTRATO:
            ```
            %s
            ```
            
            Identifica:
            - Fecha de inicio
            - Fecha de término
            - Plazos de pago
            - Periodos de notificación
            - Plazos de renovación
            - Cualquier otra fecha relevante
            
            Responde en formato JSON:
            {
              "dates": [
                {
                  "type": "Tipo de fecha (inicio, término, pago, etc.)",
                  "date": "Fecha específica o descripción del plazo",
                  "importance": "high/medium/low"
                }
              ],
              "criticalDeadlines": [
                "Fechas límite que no deben pasarse por alto"
              ]
            }
            
            RESPONDE SOLO CON EL JSON, SIN TEXTO ADICIONAL.
            """.formatted(contractText);
    }
}