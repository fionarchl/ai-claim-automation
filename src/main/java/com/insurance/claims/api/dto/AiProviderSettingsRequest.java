package com.insurance.claims.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class AiProviderSettingsRequest {
    @NotBlank
    @Size(max = 40)
    private String mode;

    @NotBlank
    @Size(max = 500)
    private String providerEndpoint;

    @NotBlank
    @Size(max = 120)
    private String providerModel;

    @DecimalMin("0.00")
    @DecimalMax("2.00")
    private BigDecimal temperature;

    @Min(1000)
    private Integer textPreviewLimit;

    @DecimalMin("0.00")
    @DecimalMax("1.00")
    private BigDecimal confidenceThreshold;

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

    public Integer getTextPreviewLimit() {
        return textPreviewLimit;
    }

    public void setTextPreviewLimit(Integer textPreviewLimit) {
        this.textPreviewLimit = textPreviewLimit;
    }

    public BigDecimal getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(BigDecimal confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }
}
