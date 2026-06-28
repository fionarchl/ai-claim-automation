package com.insurance.claims.repository;

import com.insurance.claims.domain.AiRuleSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiRuleSettingRepository extends JpaRepository<AiRuleSetting, Long> {
    Optional<AiRuleSetting> findByCode(String code);
}
