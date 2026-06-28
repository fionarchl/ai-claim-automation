package com.insurance.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_users")
public class SystemUser extends AuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "password", nullable = false, length = 120)
    private String passwordHash;

    @Column(nullable = false, length = 80)
    private String role;

    @Column(nullable = false, length = 500)
    private String permissions;

    @Column(length = 64)
    private String authTokenHash;

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

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public String getAuthTokenHash() {
        return authTokenHash;
    }

    public void setAuthTokenHash(String authTokenHash) {
        this.authTokenHash = authTokenHash;
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
