package com.insurance.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_rule_settings")
public class AiRuleSetting extends AuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, length = 20)
    private String failureOutcome;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false, length = 120)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, length = 120)
    private String updatedBy;

    public Long getId() {
        return id;
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
