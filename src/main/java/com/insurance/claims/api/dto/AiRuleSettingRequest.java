package com.insurance.claims.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiRuleSettingRequest {
    @NotBlank
    @Size(max = 80)
    private String code;

    private boolean enabled;

    @NotBlank
    @Size(max = 20)
    private String failureOutcome;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFailureOutcome() {
        return failureOutcome;
    }

    public void setFailureOutcome(String failureOutcome) {
        this.failureOutcome = failureOutcome;
    }
}
