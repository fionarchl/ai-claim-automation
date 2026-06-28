package com.insurance.claims.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AiProviderSettingsResponse {
    private String purpose;
    private String mode;
    private String providerEndpoint;
    private String providerModel;
    private boolean providerApiKeyConfigured;
    private BigDecimal temperature;
    private int textPreviewLimit;
    private BigDecimal confidenceThreshold;
    private LocalDateTime updatedAt;

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

    public boolean isProviderApiKeyConfigured() {
        return providerApiKeyConfigured;
    }

    public void setProviderApiKeyConfigured(boolean providerApiKeyConfigured) {
        this.providerApiKeyConfigured = providerApiKeyConfigured;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
