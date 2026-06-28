package com.insurance.claims.repository;

import com.insurance.claims.domain.ClaimAiAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClaimAiAssessmentRepository extends JpaRepository<ClaimAiAssessment, Long> {
    Optional<ClaimAiAssessment> findTopByClaimIdOrderByProcessedAtDesc(Long claimId);
}
