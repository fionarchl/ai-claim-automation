package com.insurance.claims.service.ai;

import com.insurance.claims.api.dto.RuleResultResponse;
import com.insurance.claims.domain.AiRecommendedDecision;

import java.util.ArrayList;
import java.util.List;

public class AiRuleEvaluation {
    private AiRecommendedDecision recommendedDecision;
    private String summary;
    private List<RuleResultResponse> ruleResults = new ArrayList<RuleResultResponse>();

    public AiRecommendedDecision getRecommendedDecision() {
        return recommendedDecision;
    }

    public void setRecommendedDecision(AiRecommendedDecision recommendedDecision) {
        this.recommendedDecision = recommendedDecision;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<RuleResultResponse> getRuleResults() {
        return ruleResults;
    }

    public void setRuleResults(List<RuleResultResponse> ruleResults) {
        this.ruleResults = ruleResults;
    }
}
