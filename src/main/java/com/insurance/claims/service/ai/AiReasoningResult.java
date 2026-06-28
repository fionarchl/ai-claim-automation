package com.insurance.claims.service.ai;

import java.util.ArrayList;
import java.util.List;

public class AiReasoningResult {
    private String summary;
    private List<String> evidence = new ArrayList<String>();
    private String modelName;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}
