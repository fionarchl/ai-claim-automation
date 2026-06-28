package com.insurance.claims.repository;

import com.insurance.claims.domain.ClaimStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimStatusHistoryRepository extends JpaRepository<ClaimStatusHistory, Long> {
    List<ClaimStatusHistory> findByClaimIdOrderByCreatedAtDesc(Long claimId);
}
