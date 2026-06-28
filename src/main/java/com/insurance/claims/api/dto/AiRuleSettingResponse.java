package com.insurance.claims.api.dto;

public class AiRuleSettingResponse {
    private String code;
    private String label;
    private boolean enabled;
    private String failureOutcome;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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
