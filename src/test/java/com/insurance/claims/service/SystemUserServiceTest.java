package com.insurance.claims.service;

import com.insurance.claims.api.dto.LoginRequest;
import com.insurance.claims.api.dto.SystemUserResponse;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.repository.SystemUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemUserServiceTest {
    @Mock
    private SystemUserRepository systemUserRepository;

    @Test
    void authenticateIssuesBearerTokenUsableForLaterRequests() {
        SystemUserService service = new SystemUserService(systemUserRepository);
        SystemUser user = user();
        when(systemUserRepository.findByUsername("jdoe")).thenReturn(Optional.of(user));
        when(systemUserRepository.save(any(SystemUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginRequest request = new LoginRequest();
        request.setUsername("jdoe");
        request.setPassword("admin123");

        SystemUserResponse response = service.authenticate(request);

        assertNotNull(response.getAuthToken());
        assertNotNull(user.getAuthTokenHash());

        when(systemUserRepository.findByAuthTokenHash(user.getAuthTokenHash())).thenReturn(Optional.of(user));
        assertEquals(user, service.findByBearerToken(response.getAuthToken()).get());
    }

    @Test
    void findByBearerTokenIgnoresMissingToken() {
        SystemUserService service = new SystemUserService(systemUserRepository);

        assertFalse(service.findByBearerToken(null).isPresent());
    }

    private SystemUser user() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setFullName("John Doe");
        user.setUsername("jdoe");
        user.setPasswordHash(passwordEncoder.encode("admin123"));
        user.setRole(RolePolicy.SYSTEM_ADMINISTRATOR);
        user.setPermissions("USERS_MANAGE");
        return user;
    }
}
