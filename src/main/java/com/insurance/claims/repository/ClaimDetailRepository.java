package com.insurance.claims.repository;

import com.insurance.claims.domain.ClaimDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimDetailRepository extends JpaRepository<ClaimDetail, Long> {
    List<ClaimDetail> findByClaimIdOrderByCreatedAtDesc(Long claimId);
    Optional<ClaimDetail> findByIdAndClaimId(Long id, Long claimId);
}
