package com.insurance.claims.api.dto;

import com.insurance.claims.domain.AiRecommendedDecision;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ClaimAiAssessmentResponse {
    private Long id;
    private Long claimId;
    private AiRecommendedDecision recommendedDecision;
    private BigDecimal confidenceScore;
    private String summary;
    private String modelName;
    private String promptVersion;
    private LocalDateTime processedAt;
    private List<RuleResultResponse> ruleResults;
    private List<String> evidence;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    public AiRecommendedDecision getRecommendedDecision() {
        return recommendedDecision;
    }

    public void setRecommendedDecision(AiRecommendedDecision recommendedDecision) {
        this.recommendedDecision = recommendedDecision;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public List<RuleResultResponse> getRuleResults() {
        return ruleResults;
    }

    public void setRuleResults(List<RuleResultResponse> ruleResults) {
        this.ruleResults = ruleResults;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
    }
}
