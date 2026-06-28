package com.insurance.claims.repository;

import com.insurance.claims.domain.PolicyBlacklistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyBlacklistEntryRepository extends JpaRepository<PolicyBlacklistEntry, Long> {
    List<PolicyBlacklistEntry> findByPolicyIdAndActiveTrue(Long policyId);
}
