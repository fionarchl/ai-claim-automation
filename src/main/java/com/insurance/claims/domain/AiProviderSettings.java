package com.insurance.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_provider_settings")
public class AiProviderSettings extends AuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40)
    private String purpose;

    @Column(nullable = false, length = 40)
    private String mode;

    @Column(nullable = false, length = 500)
    private String providerEndpoint;

    @Column(nullable = false, length = 120)
    private String providerModel;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal temperature;

    @Column(nullable = false)
    private int textPreviewLimit;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceThreshold;

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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getProviderEndpoint() {
        return providerEndpoint;
    }

    public void setProviderEndpoint(String providerEndpoint) {
        this.providerEndpoint = providerEndpoint;
    }

    public String getProviderModel() {
        return providerModel;
    }

    public void setProviderModel(String providerModel) {
        this.providerModel = providerModel;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public int getTextPreviewLimit() {
        return textPreviewLimit;
    }

    public void setTextPreviewLimit(int textPreviewLimit) {
        this.textPreviewLimit = textPreviewLimit;
    }

    public BigDecimal getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(BigDecimal confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
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
