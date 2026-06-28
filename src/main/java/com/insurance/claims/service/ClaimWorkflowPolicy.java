package com.insurance.claims.service;

import com.insurance.claims.domain.Claim;
import com.insurance.claims.domain.ClaimStatus;
import com.insurance.claims.domain.Policy;
import com.insurance.claims.domain.PolicyStatus;
import com.insurance.claims.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class ClaimWorkflowPolicy {
    private static final Map<ClaimStatus, Set<ClaimStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<ClaimStatus, Set<ClaimStatus>>(ClaimStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ClaimStatus.FILED, EnumSet.of(ClaimStatus.UNDER_REVIEW, ClaimStatus.REJECTED, ClaimStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(ClaimStatus.UNDER_REVIEW, EnumSet.of(ClaimStatus.APPROVED, ClaimStatus.REJECTED, ClaimStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(ClaimStatus.APPROVED, EnumSet.of(ClaimStatus.PAID, ClaimStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(ClaimStatus.REJECTED, EnumSet.of(ClaimStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(ClaimStatus.PAID, EnumSet.of(ClaimStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(ClaimStatus.CLOSED, EnumSet.noneOf(ClaimStatus.class));
    }

    public void validateStatusTransition(ClaimStatus current, ClaimStatus requested) {
        if (current == requested) {
            throw new BadRequestException("Claim is already in status " + requested);
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Collections.emptySet()).contains(requested)) {
            throw new BadRequestException("Claim status cannot move from " + current + " to " + requested);
        }
    }

    public void validatePolicyCanReceiveClaim(Policy policy, LocalDate admissionDate, LocalDate dischargeDate) {
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new BadRequestException("Claims can only be filed against active policies");
        }
        if (dischargeDate.isBefore(admissionDate)) {
            throw new BadRequestException("Discharge date cannot be before admission date");
        }
        if (admissionDate.isBefore(policy.getStartDate()) || dischargeDate.isAfter(policy.getEndDate())) {
            throw new BadRequestException("Admission and discharge dates must be inside the policy coverage period");
        }
    }

    public void validateApprovedAmount(Claim claim, BigDecimal approvedAmount) {
        if (approvedAmount.compareTo(claim.getPolicy().getCoverageAmount()) > 0) {
            throw new BadRequestException("Approved amount cannot exceed coverage for the admission/discharge period");
        }
    }

    public void validateClaimDetail(Claim claim,
                                    LocalDate eventStartDate,
                                    LocalDate eventEndDate,
                                    BigDecimal submittedAmount,
                                    BigDecimal approvedAmount) {
        if (eventEndDate.isBefore(eventStartDate)) {
            throw new BadRequestException("Event end date cannot be before event start date");
        }
        LocalDate claimStart = claim.getAdmissionDate() != null ? claim.getAdmissionDate() : claim.getIncidentDate();
        LocalDate claimEnd = claim.getDischargeDate() != null ? claim.getDischargeDate() : claim.getIncidentDate();
        if (eventStartDate.isBefore(claimStart) || eventEndDate.isAfter(claimEnd)) {
            throw new BadRequestException("Claim detail event dates must be inside the claim admission and discharge dates");
        }
        if (approvedAmount != null && approvedAmount.compareTo(submittedAmount) > 0) {
            throw new BadRequestException("Approved amount cannot exceed submitted amount");
        }
    }
}
