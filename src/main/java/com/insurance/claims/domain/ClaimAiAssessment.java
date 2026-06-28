package com.insurance.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "claim_ai_assessments")
public class ClaimAiAssessment extends AuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AiRecommendedDecision recommendedDecision;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(nullable = false, length = 4000)
    private String summary;

    @Lob
    @Column(nullable = false)
    private String extractedDataJson;

    @Lob
    @Column(nullable = false)
    private String ruleResultsJson;

    @Lob
    @Column(nullable = false)
    private String evidenceJson;

    @Column(nullable = false, length = 120)
    private String modelName;

    @Column(nullable = false, length = 80)
    private String promptVersion;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false, length = 120)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, length = 120)
    private String updatedBy;

    public Long getId() {
        return id;
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim claim) {
        this.claim = claim;
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

    public String getExtractedDataJson() {
        return extractedDataJson;
    }

    public void setExtractedDataJson(String extractedDataJson) {
        this.extractedDataJson = extractedDataJson;
    }

    public String getRuleResultsJson() {
        return ruleResultsJson;
    }

    public void setRuleResultsJson(String ruleResultsJson) {
        this.ruleResultsJson = ruleResultsJson;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
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

    @Override
    protected String getCreatedByValue() {
        return createdBy;
    }

    @Override
    protected void setCreatedByValue(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    protected String getUpdatedByValue() {
        return updatedBy;
    }

    @Override
    protected void setUpdatedByValue(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    protected LocalDateTime getCreatedAtValue() {
        return createdAt;
    }

    @Override
    protected void setCreatedAtValue(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    protected LocalDateTime getUpdatedAtValue() {
        return updatedAt;
    }

    @Override
    protected void setUpdatedAtValue(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
