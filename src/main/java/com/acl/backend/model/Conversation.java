package com.acl.backend.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private String title;

    @Indexed
    private Instant createdAt;

    @Indexed
    private String contractId;

    public Conversation() {
        this.createdAt = Instant.now();
    }

    public Conversation(Long userId, String title) {
        this.userId = userId;
        this.title = title;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }
}
