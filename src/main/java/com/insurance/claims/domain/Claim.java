package com.insurance.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
public class Claim extends AuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(nullable = false)
    private LocalDate incidentDate;

    private LocalDate admissionDate;

    private LocalDate dischargeDate;

    @Column(nullable = false)
    private LocalDate filedDate;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 2000)
    private String claimDocuments;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(precision = 14, scale = 2)
    private BigDecimal approvedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ClaimStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false, length = 120)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, length = 120)
    private String updatedBy;

    @PrePersist
    void prePersist() {
        if (filedDate == null) {
            filedDate = LocalDate.now();
        }
        if (status == null) {
            status = ClaimStatus.FILED;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public LocalDate getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(LocalDate incidentDate) {
        this.incidentDate = incidentDate;
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

    public LocalDate getFiledDate() {
        return filedDate;
    }

    public void setFiledDate(LocalDate filedDate) {
        this.filedDate = filedDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClaimDocuments() {
        return claimDocuments;
    }

    public void setClaimDocuments(String claimDocuments) {
        this.claimDocuments = claimDocuments;
    }

    public BigDecimal getEstimatedAmount() {
        return estimatedAmount;
    }

    public void setEstimatedAmount(BigDecimal estimatedAmount) {
        this.estimatedAmount = estimatedAmount;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    @Override
    protected String getCreatedByValue() {
        return createdBy;
    }

    @Override
    protected void setCreatedByValue(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    protected String getUpdatedByValue() {
        return updatedBy;
    }

    @Override
    protected void setUpdatedByValue(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    protected LocalDateTime getCreatedAtValue() {
        return createdAt;
    }

    @Override
    protected void setCreatedAtValue(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    protected LocalDateTime getUpdatedAtValue() {
        return updatedAt;
    }

    @Override
    protected void setUpdatedAtValue(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
