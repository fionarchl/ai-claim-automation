package com.insurance.claims.api;

import com.insurance.claims.api.dto.SystemUserRequest;
import com.insurance.claims.api.dto.SystemUserResponse;
import com.insurance.claims.api.dto.SystemUserUpdateRequest;
import com.insurance.claims.api.dto.UserPermissionsRequest;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.exception.ForbiddenException;
import com.insurance.claims.service.RolePolicy;
import com.insurance.claims.service.SystemUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class SystemUserController {
    private final SystemUserService systemUserService;

    public SystemUserController(SystemUserService systemUserService) {
        this.systemUserService = systemUserService;
    }

    @GetMapping
    public List<SystemUserResponse> findAll(@AuthenticationPrincipal SystemUser user) {
        requireUserManager(user);
        return systemUserService.findAll();
    }

    @GetMapping("/{id}")
    public SystemUserResponse findById(@PathVariable Long id,
                                       @AuthenticationPrincipal SystemUser user) {
        requireUserManager(user);
        return systemUserService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SystemUserResponse create(@Valid @RequestBody SystemUserRequest request,
                                     @AuthenticationPrincipal SystemUser user) {
        SystemUser manager = requireUserManager(user);
        return systemUserService.create(request, actorName(manager));
    }

    @PatchMapping("/{id}/permissions")
    public SystemUserResponse updatePermissions(@PathVariable Long id,
                                                @Valid @RequestBody UserPermissionsRequest request,
                                                @AuthenticationPrincipal SystemUser user) {
        SystemUser manager = requireUserManager(user);
        return systemUserService.updatePermissions(id, request, actorName(manager));
    }

    @PatchMapping("/{id}")
    public SystemUserResponse update(@PathVariable Long id,
                                     @Valid @RequestBody SystemUserUpdateRequest request,
                                     @AuthenticationPrincipal SystemUser user) {
        SystemUser manager = requireUserManager(user);
        return systemUserService.update(id, request, actorName(manager));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @AuthenticationPrincipal SystemUser user) {
        SystemUser manager = requireUserManager(user);
        systemUserService.delete(id, manager.getId());
    }

    private SystemUser requireUserManager(SystemUser user) {
        if (!RolePolicy.canManageUsers(user)) {
            throw new ForbiddenException("Only System Administrator can manage users");
        }
        return user;
    }

    private String actorName(SystemUser user) {
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            return user.getFullName();
        }
        return user.getUsername();
    }
}
