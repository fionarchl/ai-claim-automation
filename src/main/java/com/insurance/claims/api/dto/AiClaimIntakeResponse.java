package com.insurance.claims.api.dto;

public class AiClaimIntakeResponse {
    private ClaimResponse claim;
    private ClaimAiAssessmentResponse assessment;

    public ClaimResponse getClaim() {
        return claim;
    }

    public void setClaim(ClaimResponse claim) {
        this.claim = claim;
    }

    public ClaimAiAssessmentResponse getAssessment() {
        return assessment;
    }

    public void setAssessment(ClaimAiAssessmentResponse assessment) {
        this.assessment = assessment;
    }
}
