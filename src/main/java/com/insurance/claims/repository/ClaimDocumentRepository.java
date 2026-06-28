package com.insurance.claims.repository;

import com.insurance.claims.domain.ClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {
    List<ClaimDocument> findByClaimIdOrderByCreatedAtDesc(Long claimId);
    Optional<ClaimDocument> findByIdAndClaimId(Long id, Long claimId);
}
