package com.acl.backend.data;

import java.util.List;

public class AnalysisData {

    public static class AnalyzeTextRequest {
        private String name;
        private String text;
        private Long userId;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class AnalysisResult {
        private String type;
        private List<String> keyClauses;
        private List<String> risks;
        private double riskScore;
        private List<String> recommendations;
        private String summary;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public List<String> getKeyClauses() { return keyClauses; }
        public void setKeyClauses(List<String> keyClauses) { this.keyClauses = keyClauses; }
        public List<String> getRisks() { return risks; }
        public void setRisks(List<String> risks) { this.risks = risks; }
        public double getRiskScore() { return riskScore; }
        public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }

    public static class UploadResponse {
        private String contractId;
        private AnalysisResult analysis;

        public String getContractId() { return contractId; }
        public void setContractId(String contractId) { this.contractId = contractId; }
        public AnalysisResult getAnalysis() { return analysis; }
        public void setAnalysis(AnalysisResult analysis) { this.analysis = analysis; }
    }
}