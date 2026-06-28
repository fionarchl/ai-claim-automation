package com.insurance.claims.api.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class UserPermissionsRequest {

    @NotEmpty
    private List<String> permissions;

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
