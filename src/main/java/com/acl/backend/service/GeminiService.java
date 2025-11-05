package com.acl.backend.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.acl.backend.data.GeminiData.*;
import com.acl.backend.data.GeminiData.GeminiResponse;
import com.acl.backend.data.GeminiData.GeminiRequest;

import com.google.gson.Gson;

import reactor.core.publisher.Mono;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final WebClient webClient;
    private final Gson gson;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.max.tokens:8000}")
    private int maxTokens;

    @Value("${gemini.temperature:0.3}")
    private double temperature;

    public GeminiService(WebClient.Builder webClientBuilder){
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.gson = new Gson();
    }

    // Genera contenido usando Gemini API
    public String generateContent(String prompt) {
        try {
            log.info("Llamando Api");

            GeminiRequest request = new GeminiRequest(prompt, temperature, maxTokens);

            String endpoint = String.format("/v1beta/models/%s:generateContent?key=%s",
                                        model, apiKey);

            GeminiResponse response = webClient.post()
                    .uri(endpoint)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            if(response != null){
                String result = response.getGeneratedText();
                log.info("Respuesta de Gemini: {}", result);
                return result;
            }

            throw  new RuntimeException("Respuesta nula de Gemini API");
        } catch (Exception e) {
            log.error("Error al llamar a Gemini API: {}", e.getMessage());
            throw new RuntimeException("Error al generar contenido con Gemini API" + e.getMessage(), e);
        }
    }

    public Mono<String> generateContentAsync(String prompt) {
        try {
            GeminiRequest request = new GeminiRequest(prompt, temperature, maxTokens);

            String endpoint = String.format("/v1beta/models/%s:generateContent?key=%s",
                    model, apiKey);

            return webClient.post()
                    .uri(endpoint)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofSeconds(60))
                    .map(GeminiResponse::getGeneratedText)
                    .doOnError(e -> log.error("Error en llamada async a Gemini: {}", e.getMessage()));

        } catch (Exception e) {
            log.error("Error preparando llamada async: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    // Valida que el texto no exceda el limite de tokens
    public boolean ValidateTextLength(String text){
        // 1 token = 4 caracteres aprox.
        int estimatedTokens = text.length() / 4;
        return estimatedTokens <= (maxTokens * 0.6);
        // Dejamos un 40% de margen para la respuesta
    }

    // Trunca el texto si es muy largo
    public String truncateIfNeeded(String text, int maxChars) {
        if(text.length() > maxChars){
            log.warn("Texto truncado de {} a {} caracteres", text.length(), maxChars);
            return text.substring(0, maxChars) + "\n\n[TEXTO TRUNCADO]";
        }
        return text;
    }
}
