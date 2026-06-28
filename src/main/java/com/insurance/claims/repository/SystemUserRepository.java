package com.insurance.claims.repository;

import com.insurance.claims.domain.SystemUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemUserRepository extends JpaRepository<SystemUser, Long> {
    Optional<SystemUser> findByUsername(String username);
    Optional<SystemUser> findByAuthTokenHash(String authTokenHash);
    boolean existsByUsername(String username);
}
