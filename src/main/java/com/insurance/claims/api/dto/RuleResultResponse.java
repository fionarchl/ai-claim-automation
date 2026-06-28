package com.insurance.claims.api.dto;

public class RuleResultResponse {
    private String code;
    private String label;
    private String outcome;
    private String message;

    public RuleResultResponse() {
    }

    public RuleResultResponse(String code, String label, String outcome, String message) {
        this.code = code;
        this.label = label;
        this.outcome = outcome;
        this.message = message;
    }

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

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
