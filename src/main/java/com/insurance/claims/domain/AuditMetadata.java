package com.insurance.claims.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class AuditMetadata {
    private static final String SYSTEM_ACTOR = "system";

    @PrePersist
    void auditPrePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (getCreatedAtValue() == null) {
            setCreatedAtValue(now);
        }
        if (getUpdatedAtValue() == null) {
            setUpdatedAtValue(now);
        }
        if (getCreatedByValue() == null || getCreatedByValue().trim().isEmpty()) {
            setCreatedByValue(SYSTEM_ACTOR);
        }
        if (getUpdatedByValue() == null || getUpdatedByValue().trim().isEmpty()) {
            setUpdatedByValue(getCreatedByValue());
        }
    }

    @PreUpdate
    void auditPreUpdate() {
        setUpdatedAtValue(LocalDateTime.now());
        if (getUpdatedByValue() == null || getUpdatedByValue().trim().isEmpty()) {
            setUpdatedByValue(SYSTEM_ACTOR);
        }
    }

    public void markCreatedBy(String actor) {
        String normalized = normalizeActor(actor);
        setCreatedByValue(normalized);
        setUpdatedByValue(normalized);
    }

    public void markUpdatedBy(String actor) {
        setUpdatedByValue(normalizeActor(actor));
    }

    private String normalizeActor(String actor) {
        return actor == null || actor.trim().isEmpty() ? SYSTEM_ACTOR : actor.trim();
    }

    public String getCreatedBy() {
        return getCreatedByValue();
    }

    public String getUpdatedBy() {
        return getUpdatedByValue();
    }

    public LocalDateTime getCreatedAt() {
        return getCreatedAtValue();
    }

    public LocalDateTime getUpdatedAt() {
        return getUpdatedAtValue();
    }

    protected abstract String getCreatedByValue();

    protected abstract void setCreatedByValue(String createdBy);

    protected abstract String getUpdatedByValue();

    protected abstract void setUpdatedByValue(String updatedBy);

    protected abstract LocalDateTime getCreatedAtValue();

    protected abstract void setCreatedAtValue(LocalDateTime createdAt);

    protected abstract LocalDateTime getUpdatedAtValue();

    protected abstract void setUpdatedAtValue(LocalDateTime updatedAt);
}
