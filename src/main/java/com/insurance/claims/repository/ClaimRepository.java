package com.insurance.claims.repository;

import com.insurance.claims.domain.Claim;
import com.insurance.claims.domain.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimNumber(String claimNumber);
    boolean existsByClaimNumber(String claimNumber);
    List<Claim> findByStatus(ClaimStatus status);
    List<Claim> findByPolicyId(Long policyId);
    List<Claim> findByPolicyIdAndStatus(Long policyId, ClaimStatus status);
}
