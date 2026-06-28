package com.insurance.claims.service;

import com.insurance.claims.api.dto.LoginRequest;
import com.insurance.claims.api.dto.SystemUserResponse;
import com.insurance.claims.api.dto.SystemUserRequest;
import com.insurance.claims.api.dto.SystemUserUpdateRequest;
import com.insurance.claims.api.dto.UserPermissionsRequest;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.exception.BadRequestException;
import com.insurance.claims.exception.ForbiddenException;
import com.insurance.claims.exception.ResourceNotFoundException;
import com.insurance.claims.repository.SystemUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SystemUserService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SystemUserRepository systemUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public SystemUserService(SystemUserRepository systemUserRepository) {
        this.systemUserRepository = systemUserRepository;
    }

    @Transactional(readOnly = true)
    public List<SystemUserResponse> findAll() {
        return systemUserRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SystemUserResponse findById(Long id) {
        return toResponse(getUser(id));
    }

    @Transactional
    public SystemUserResponse authenticate(LoginRequest request) {
        SystemUser user = systemUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid username or password");
        }
        String token = generateToken();
        user.setAuthTokenHash(hashToken(token));
        user.markUpdatedBy(actorName(user));
        SystemUserResponse response = toResponse(systemUserRepository.save(user));
        response.setAuthToken(token);
        return response;
    }

    @Transactional
    public SystemUserResponse create(SystemUserRequest request) {
        return create(request, "system");
    }

    @Transactional
    public SystemUserResponse create(SystemUserRequest request, String actor) {
        RolePolicy.validateRole(request.getRole());
        if (systemUserRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists: " + request.getUsername());
        }

        SystemUser user = new SystemUser();
        user.setFullName(request.getFullName());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setPermissions(String.join(",", request.getPermissions()));
        user.markCreatedBy(actor);
        return toResponse(systemUserRepository.save(user));
    }

    @Transactional
    public SystemUserResponse updatePermissions(Long id, UserPermissionsRequest request) {
        return updatePermissions(id, request, "system");
    }

    @Transactional
    public SystemUserResponse updatePermissions(Long id, UserPermissionsRequest request, String actor) {
        SystemUser user = getUser(id);
        user.setPermissions(String.join(",", request.getPermissions()));
        user.markUpdatedBy(actor);
        return toResponse(systemUserRepository.save(user));
    }

    @Transactional
    public SystemUserResponse update(Long id, SystemUserUpdateRequest request) {
        return update(id, request, "system");
    }

    @Transactional
    public SystemUserResponse update(Long id, SystemUserUpdateRequest request, String actor) {
        RolePolicy.validateRole(request.getRole());
        SystemUser user = getUser(id);
        systemUserRepository.findByUsername(request.getUsername())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Username already exists: " + request.getUsername());
                });
        user.setFullName(request.getFullName());
        user.setUsername(request.getUsername());
        user.setRole(request.getRole());
        user.setPermissions(String.join(",", request.getPermissions()));
        user.markUpdatedBy(actor);
        return toResponse(systemUserRepository.save(user));
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new ForbiddenException("You cannot delete your own signed-in user");
        }
        SystemUser user = getUser(id);
        systemUserRepository.delete(user);
    }

    private SystemUser getUser(Long id) {
        return systemUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<SystemUser> findByBearerToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }
        return systemUserRepository.findByAuthTokenHash(hashToken(token.trim()));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private SystemUserResponse toResponse(SystemUser user) {
        SystemUserResponse response = new SystemUserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setPermissions(toPermissions(user.getPermissions()));
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private String actorName(SystemUser user) {
        return user.getFullName() != null && !user.getFullName().trim().isEmpty()
                ? user.getFullName()
                : user.getUsername();
    }

    private List<String> toPermissions(String permissions) {
        if (permissions == null || permissions.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(permissions.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }
}
