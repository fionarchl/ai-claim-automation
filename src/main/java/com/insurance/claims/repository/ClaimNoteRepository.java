package com.insurance.claims.repository;

import com.insurance.claims.domain.ClaimNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimNoteRepository extends JpaRepository<ClaimNote, Long> {
    List<ClaimNote> findByClaimIdOrderByCreatedAtDesc(Long claimId);
}
