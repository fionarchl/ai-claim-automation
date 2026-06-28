package com.insurance.claims.service.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AiClaimExtraction {
    private String policyNumber;
    private String customerName;
    private LocalDate admissionDate;
    private LocalDate dischargeDate;
    private BigDecimal estimatedAmount;
    private String description;
    private BigDecimal confidence;
    private List<String> evidence = new ArrayList<String>();
    private List<String> riskIndicators = new ArrayList<String>();
    private List<AiClaimDetailExtraction> claimDetails = new ArrayList<AiClaimDetailExtraction>();

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDate admissionDate) {
        this.admissionDate = admissionDate;
    }

    public LocalDate getDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(LocalDate dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public BigDecimal getEstimatedAmount() {
        return estimatedAmount;
    }

    public void setEstimatedAmount(BigDecimal estimatedAmount) {
        this.estimatedAmount = estimatedAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
    }

    public List<String> getRiskIndicators() {
        return riskIndicators;
    }

    public void setRiskIndicators(List<String> riskIndicators) {
        this.riskIndicators = riskIndicators;
    }

    public List<AiClaimDetailExtraction> getClaimDetails() {
        return claimDetails;
    }

    public void setClaimDetails(List<AiClaimDetailExtraction> claimDetails) {
        this.claimDetails = claimDetails;
    }
}
