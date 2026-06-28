package com.insurance.claims.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class AiClaimIntakeRequest {

    private Long policyId;

    @Size(max = 60)
    private String policyNumber;

    @Size(max = 1000)
    private String userNote;

    @NotEmpty
    private List<@Valid ClaimDocumentRequest> documents;

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getUserNote() {
        return userNote;
    }

    public void setUserNote(String userNote) {
        this.userNote = userNote;
    }

    public List<ClaimDocumentRequest> getDocuments() {
        return documents;
    }

    public void setDocuments(List<ClaimDocumentRequest> documents) {
        this.documents = documents;
    }
}
