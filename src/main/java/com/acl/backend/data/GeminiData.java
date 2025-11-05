package com.acl.backend.data;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class GeminiData {
    public static class GeminiRequest {
        private List<Content> contents;
        private GenerationConfig generationConfig;

        public GeminiRequest(String text) {
            this.contents = List.of(new Content(List.of(new Part(text))));
            this.generationConfig = new GenerationConfig();
        }

        public GeminiRequest(String text, double temperature, int maxTokens) {
            this.contents = List.of(new Content(List.of(new Part(text))));
            this.generationConfig = new GenerationConfig(temperature, maxTokens);
        }

        public List<Content> getContents() { return contents; }
        public void setContents(List<Content> contents) { this.contents = contents; }
        public GenerationConfig getGenerationConfig() { return generationConfig; }
        public void setGenerationConfig(GenerationConfig config) { this.generationConfig = config; }
    }

    public static class Content {
        private List<Part> parts;

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() { return parts; }
        public void setParts(List<Part> parts) { this.parts = parts; }
    }

    public static class Part {
        private String text;

        public Part(String text) {
            this.text = text;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class GenerationConfig {
        private Double temperature;
        private Integer maxOutputTokens;
        @SerializedName("topK")
        private Integer topK;
        @SerializedName("topP")
        private Double topP;

        public GenerationConfig() {
            this.temperature = 0.3;
            this.maxOutputTokens = 8000;
            this.topK = 40;
            this.topP = 0.95;
        }

        public GenerationConfig(double temperature, int maxTokens) {
            this.temperature = temperature;
            this.maxOutputTokens = maxTokens;
            this.topK = 40;
            this.topP = 0.95;
        }

        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Integer getMaxOutputTokens() { return maxOutputTokens; }
        public void setMaxOutputTokens(Integer maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
        public Integer getTopK() { return topK; }
        public void setTopK(Integer topK) { this.topK = topK; }
        public Double getTopP() { return topP; }
        public void setTopP(Double topP) { this.topP = topP; }
    }

    // ========== RESPONSE ==========
    public static class GeminiResponse {
        private List<Candidate> candidates;
        private UsageMetadata usageMetadata;

        public List<Candidate> getCandidates() { return candidates; }
        public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }
        public UsageMetadata getUsageMetadata() { return usageMetadata; }
        public void setUsageMetadata(UsageMetadata usageMetadata) { this.usageMetadata = usageMetadata; }

        public String getGeneratedText() {
            if (candidates != null && !candidates.isEmpty()) {
                Candidate first = candidates.get(0);
                if (first.getContent() != null && first.getContent().getParts() != null
                        && !first.getContent().getParts().isEmpty()) {
                    return first.getContent().getParts().get(0).getText();
                }
            }
            return "";
        }
    }

    public static class Candidate {
        private Content content;
        private String finishReason;
        private Integer index;
        private List<SafetyRating> safetyRatings;

        public Content getContent() { return content; }
        public void setContent(Content content) { this.content = content; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        public List<SafetyRating> getSafetyRatings() { return safetyRatings; }
        public void setSafetyRatings(List<SafetyRating> safetyRatings) { this.safetyRatings = safetyRatings; }
    }

    public static class SafetyRating {
        private String category;
        private String probability;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getProbability() { return probability; }
        public void setProbability(String probability) { this.probability = probability; }
    }

    public static class UsageMetadata {
        private Integer promptTokenCount;
        private Integer candidatesTokenCount;
        private Integer totalTokenCount;

        public Integer getPromptTokenCount() { return promptTokenCount; }
        public void setPromptTokenCount(Integer promptTokenCount) { this.promptTokenCount = promptTokenCount; }
        public Integer getCandidatesTokenCount() { return candidatesTokenCount; }
        public void setCandidatesTokenCount(Integer candidatesTokenCount) { this.candidatesTokenCount = candidatesTokenCount; }
        public Integer getTotalTokenCount() { return totalTokenCount; }
        public void setTotalTokenCount(Integer totalTokenCount) { this.totalTokenCount = totalTokenCount; }
    }
}
