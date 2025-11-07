package com.acl.backend.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acl.backend.model.ChatMessage;
import com.acl.backend.model.Contract;
import com.acl.backend.model.User;
import com.acl.backend.repository.ChatRepository;
import com.acl.backend.repository.UserRepository;
import com.acl.backend.service.AIAnalysisService;
import com.acl.backend.service.ContractService;
import com.acl.backend.service.NLPAnalysisService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final NLPAnalysisService nlpAnalysisService;
    private final AIAnalysisService aiAnalysisService;
    private final ContractService contractService;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    public ChatController(
            NLPAnalysisService nlpAnalysisService,
            AIAnalysisService aiAnalysisService,
            ContractService contractService,
            UserRepository userRepository,
            ChatRepository chatRepository) {
        this.nlpAnalysisService = nlpAnalysisService;
        this.aiAnalysisService = aiAnalysisService;
        this.contractService = contractService;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
    }

    // Chat sobre un contrato específico con historial
    @PostMapping("/{contractId}")
    public ResponseEntity<ChatResponse> chat(
            @PathVariable String contractId,
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Validar acceso al contrato
        Contract contract = contractService.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        Long userId = null;
        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            userId = user.getId();

            if (contract.getUserId() != null && !contract.getUserId().equals(userId)) {
                return ResponseEntity.status(403).build();
            }
        }

        // Guardar mensaje del usuario
        ChatMessage userMessage = new ChatMessage();
        userMessage.setContractId(contractId);
        userMessage.setUserId(userId);
        userMessage.setMessage(request.getMessage());
        userMessage.setRole("user");
        userMessage.setTimestamp(Instant.now());
        chatRepository.save(userMessage);

        // Obtener respuesta de la IA
        List<String> answers = nlpAnalysisService.answerQuestions(
                contract.getContent(),
                request.getMessage()
        );

        String responseText = String.join("\n\n", answers);

        // Guardar respuesta del asistente
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setContractId(contractId);
        assistantMessage.setUserId(userId);
        assistantMessage.setMessage(responseText);
        assistantMessage.setRole("assistant");
        assistantMessage.setTimestamp(Instant.now());
        chatRepository.save(assistantMessage);

        // Generar sugerencias de preguntas
        List<String> suggestions = generateSuggestions(contract.getType());

        ChatResponse response = new ChatResponse();
        response.setMessage(responseText);
        response.setContractName(contract.getName());
        response.setTimestamp(Instant.now());
        response.setSuggestions(suggestions);

        return ResponseEntity.ok(response);
    }


    // Chat legal general (sin contrato específico)
    @PostMapping("/general")
    public ResponseEntity<ChatResponse> generalChat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = null;
        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            userId = user.getId();
        }

        // Guardar mensaje del usuario
        ChatMessage userMessage = new ChatMessage();
        userMessage.setUserId(userId);
        userMessage.setMessage(request.getMessage());
        userMessage.setRole("user");
        userMessage.setTimestamp(Instant.now());
        chatRepository.save(userMessage);

        // Usar prompt específico para preguntas legales generales
        String prompt = buildGeneralLegalPrompt(request.getMessage());
        String answer = aiAnalysisService.generateContent(prompt);

        // Guardar respuesta del asistente
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setUserId(userId);
        assistantMessage.setMessage(answer);
        assistantMessage.setRole("assistant");
        assistantMessage.setTimestamp(Instant.now());
        chatRepository.save(assistantMessage);

        ChatResponse response = new ChatResponse();
        response.setMessage(answer);
        response.setTimestamp(Instant.now());
        response.setSuggestions(List.of(
                "¿Qué es una cláusula de confidencialidad?",
                "¿Cuáles son los elementos esenciales de un contrato?",
                "¿Qué es la jurisdicción en un contrato?"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener historial de chat de un contrato
     */
    @GetMapping("/{contractId}/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable String contractId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Validar acceso
        Contract contract = contractService.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (contract.getUserId() != null && !contract.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }

        List<ChatMessage> history = chatRepository.findByContractIdOrderByTimestampAsc(contractId);
        return ResponseEntity.ok(history);
    }

    /**
     * Obtener todo el historial de chat del usuario
     */
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getUserChatHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<ChatMessage> history = chatRepository.findByUserIdOrderByTimestampDesc(user.getId());
        return ResponseEntity.ok(history);
    }

    /**
     * Limpiar historial de un contrato
     */
    @PostMapping("/{contractId}/clear")
    public ResponseEntity<Void> clearChatHistory(
            @PathVariable String contractId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Contract contract = contractService.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));

        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (contract.getUserId() != null && !contract.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }

        chatRepository.deleteByContractId(contractId);
        return ResponseEntity.ok().build();
    }

    // Metodos Auxiliares

    private String buildGeneralLegalPrompt(String question) {
        return String.format("""
            Eres un asistente legal experto. Responde la siguiente pregunta legal de manera clara y precisa.
            
            PREGUNTA:
            %s
            
            Proporciona una respuesta profesional y educativa. Si la pregunta requiere asesoría legal específica,
            recomienda consultar con un abogado profesional.
            
            Responde de forma directa, sin formato JSON.
            """, question);
    }

    private List<String> generateSuggestions(String contractType) {
        List<String> suggestions = new ArrayList<>();

        switch (contractType != null ? contractType : "General") {
            case "Laboral":
                suggestions.add("¿Cuál es el período de prueba?");
                suggestions.add("¿Qué beneficios incluye el contrato?");
                suggestions.add("¿Cuáles son las causales de terminación?");
                break;
            case "Arrendamiento":
                suggestions.add("¿Cuál es el monto del canon de arrendamiento?");
                suggestions.add("¿Quién paga los servicios públicos?");
                suggestions.add("¿Cuál es el plazo del contrato?");
                break;
            case "Servicios":
                suggestions.add("¿Cuáles son los entregables del servicio?");
                suggestions.add("¿Qué incluyen las condiciones de pago?");
                suggestions.add("¿Hay garantías o SLAs definidos?");
                break;
            default:
                suggestions.add("¿Cuáles son las obligaciones principales?");
                suggestions.add("¿Qué riesgos tiene este contrato?");
                suggestions.add("¿Cómo se puede terminar el contrato?");
        }

        return suggestions;
    }

    // Data

    public static class ChatRequest {
        @NotBlank(message = "El mensaje no puede estar vacío")
        private String message;
        private String conversationId;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    }

    public static class ChatResponse {
        private String message;
        private String contractName;
        private Instant timestamp;
        private List<String> suggestions;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getContractName() { return contractName; }
        public void setContractName(String contractName) { this.contractName = contractName; }
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    }
}