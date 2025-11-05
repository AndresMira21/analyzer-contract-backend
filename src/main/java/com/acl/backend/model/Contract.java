package com.acl.backend.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "contracts")
public class Contract {
    @Id
    private String id;

    private String name;
    private String type;
    private String content;

    private List<String> keyClauses;
    private List<String> risks;

    private double riskScore;

    private Instant uploadedAt = Instant.now();

    private Long userId;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getKeyClauses() {
        return keyClauses;
    }
    public void setKeyClauses(List<String> keyClauses) {
        this.keyClauses = keyClauses;
    }

    public List<String> getRisks() {
        return risks;
    }
    public void setRisks(List<String> risks) {
        this.risks = risks;
    }

    public Double getRiskScore() {
        return riskScore;
    }
    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
