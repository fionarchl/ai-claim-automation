package com.insurance.claims.repository;

import com.insurance.claims.domain.AiProviderSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiProviderSettingsRepository extends JpaRepository<AiProviderSettings, Long> {
    Optional<AiProviderSettings> findByPurpose(String purpose);
    Optional<AiProviderSettings> findTopByOrderByIdAsc();
}
