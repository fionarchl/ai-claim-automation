package com.insurance.claims.api;

import com.insurance.claims.api.dto.LoginRequest;
import com.insurance.claims.api.dto.SystemUserResponse;
import com.insurance.claims.service.SystemUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final SystemUserService systemUserService;

    public AuthController(SystemUserService systemUserService) {
        this.systemUserService = systemUserService;
    }

    @PostMapping("/login")
    public SystemUserResponse login(@Valid @RequestBody LoginRequest request) {
        return systemUserService.authenticate(request);
    }
}
