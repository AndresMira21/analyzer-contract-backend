package com.acl.backend.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "chatMessages")
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String contractId; // Puede ser null para chat generales

    @Indexed
    private Long userId;

    private String message;

    private String role; // User o Assistant

    @Indexed
    private Instant timestamp;

    private String conversationId;

    public ChatMessage() {
        this.timestamp = Instant.now();
    }

    public ChatMessage(String contractId, Long userId, String message, String role) {
        this.contractId = contractId;
        this.userId = userId;
        this.message = message;
        this.role = role;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getContractId() {
        return contractId;
    }
    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getConversationId() {
        return conversationId;
    }
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
