package com.insurance.claims.service;

import com.insurance.claims.domain.Claim;
import com.insurance.claims.domain.ClaimDetail;
import com.insurance.claims.repository.ClaimDetailRepository;
import com.insurance.claims.repository.ClaimRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ClaimAmountService {
    private final ClaimDetailRepository claimDetailRepository;
    private final ClaimRepository claimRepository;
    private final ClaimWorkflowPolicy claimWorkflowPolicy;

    public ClaimAmountService(ClaimDetailRepository claimDetailRepository,
                              ClaimRepository claimRepository,
                              ClaimWorkflowPolicy claimWorkflowPolicy) {
        this.claimDetailRepository = claimDetailRepository;
        this.claimRepository = claimRepository;
        this.claimWorkflowPolicy = claimWorkflowPolicy;
    }

    public BigDecimal totalApprovedDetailAmount(Long claimId) {
        return claimDetailRepository.findByClaimIdOrderByCreatedAtDesc(claimId)
                .stream()
                .map(ClaimDetail::getApprovedAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void syncClaimApprovedAmount(Claim claim, String actor) {
        BigDecimal approvedTotal = totalApprovedDetailAmount(claim.getId());
        claimWorkflowPolicy.validateApprovedAmount(claim, approvedTotal);
        claim.setApprovedAmount(approvedTotal);
        claim.markUpdatedBy(actor);
        claimRepository.save(claim);
    }
}
