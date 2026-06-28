package com.insurance.claims.api.dto;

import java.util.List;

public class AiConfigurationResponse {
    private AiProviderSettingsResponse extractionSettings;
    private AiProviderSettingsResponse reasoningSettings;
    private List<AiRuleSettingResponse> ruleSettings;

    public AiProviderSettingsResponse getExtractionSettings() {
        return extractionSettings;
    }

    public void setExtractionSettings(AiProviderSettingsResponse extractionSettings) {
        this.extractionSettings = extractionSettings;
    }

    public AiProviderSettingsResponse getReasoningSettings() {
        return reasoningSettings;
    }

    public void setReasoningSettings(AiProviderSettingsResponse reasoningSettings) {
        this.reasoningSettings = reasoningSettings;
    }

    public AiProviderSettingsResponse getProviderSettings() {
        return extractionSettings;
    }

    public void setProviderSettings(AiProviderSettingsResponse providerSettings) {
        this.extractionSettings = providerSettings;
    }

    public List<AiRuleSettingResponse> getRuleSettings() {
        return ruleSettings;
    }

    public void setRuleSettings(List<AiRuleSettingResponse> ruleSettings) {
        this.ruleSettings = ruleSettings;
    }
}
