package com.insurance.claims.service;

import com.insurance.claims.api.dto.ClaimDetailRequest;
import com.insurance.claims.api.dto.ClaimStatusUpdateRequest;
import com.insurance.claims.domain.Claim;
import com.insurance.claims.domain.ClaimStatus;
import com.insurance.claims.domain.Customer;
import com.insurance.claims.domain.Policy;
import com.insurance.claims.domain.PolicyStatus;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.exception.BadRequestException;
import com.insurance.claims.exception.ForbiddenException;
import com.insurance.claims.repository.ClaimDetailRepository;
import com.insurance.claims.repository.ClaimNoteRepository;
import com.insurance.claims.repository.ClaimRepository;
import com.insurance.claims.repository.ClaimStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {
    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ClaimNoteRepository claimNoteRepository;

    @Mock
    private ClaimDetailRepository claimDetailRepository;

    @Mock
    private ClaimStatusHistoryRepository claimStatusHistoryRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private ClaimDocumentService claimDocumentService;

    @Mock
    private ClaimAmountService claimAmountService;

    @Mock
    private ClaimMapper claimMapper;

    private ClaimService claimService;

    @BeforeEach
    void setUp() {
        claimService = new ClaimService(
                claimRepository,
                claimNoteRepository,
                claimDetailRepository,
                claimStatusHistoryRepository,
                policyService,
                new ClaimWorkflowPolicy(),
                claimDocumentService,
                claimAmountService,
                claimMapper);
    }

    @Test
    void updateStatusRejectsInvalidWorkflowTransition() {
        Claim claim = claim(ClaimStatus.FILED);
        SystemUser user = user(RolePolicy.CLAIM_ANALYST);
        when(claimRepository.findById(7L)).thenReturn(Optional.of(claim));

        ClaimStatusUpdateRequest request = new ClaimStatusUpdateRequest();
        request.setStatus(ClaimStatus.APPROVED);
        request.setChangedBy("Analyst");

        assertThrows(BadRequestException.class, () -> claimService.updateStatus(7L, request, user));
        verify(claimRepository, never()).save(claim);
    }

    @Test
    void addDetailRejectsFinalizedClaim() {
        Claim claim = claim(ClaimStatus.APPROVED);
        SystemUser user = user(RolePolicy.CLAIM_ANALYST);
        when(claimRepository.findById(7L)).thenReturn(Optional.of(claim));

        ClaimDetailRequest request = new ClaimDetailRequest();
        request.setCategory("Room and Board");
        request.setEventStartDate(LocalDate.of(2026, 6, 10));
        request.setEventEndDate(LocalDate.of(2026, 6, 11));
        request.setSubmittedAmount(new BigDecimal("100.00"));
        request.setApprovedAmount(new BigDecimal("75.00"));

        assertThrows(ForbiddenException.class, () -> claimService.addDetail(7L, request, user));
    }

    @Test
    void deleteDocumentRejectsFinalizedClaim() {
        Claim claim = claim(ClaimStatus.PAID);
        SystemUser user = user(RolePolicy.CLAIM_ANALYST);
        when(claimRepository.findById(7L)).thenReturn(Optional.of(claim));

        assertThrows(ForbiddenException.class, () -> claimService.deleteDocument(7L, 9L, user));
        verify(claimDocumentService, never()).deleteDocument(7L, 9L);
    }

    private Claim claim(ClaimStatus status) {
        Customer customer = new Customer();
        customer.setId(3L);
        customer.setFullName("Jane Customer");

        Policy policy = new Policy();
        policy.setId(2L);
        policy.setCustomer(customer);
        policy.setPolicyNumber("POL-1");
        policy.setCoverageAmount(new BigDecimal("1000.00"));
        policy.setStartDate(LocalDate.of(2026, 1, 1));
        policy.setEndDate(LocalDate.of(2026, 12, 31));
        policy.setStatus(PolicyStatus.ACTIVE);

        Claim claim = new Claim();
        claim.setId(7L);
        claim.setPolicy(policy);
        claim.setClaimNumber("CLM-1");
        claim.setAdmissionDate(LocalDate.of(2026, 6, 10));
        claim.setDischargeDate(LocalDate.of(2026, 6, 11));
        claim.setIncidentDate(LocalDate.of(2026, 6, 10));
        claim.setStatus(status);
        return claim;
    }

    private SystemUser user(String role) {
        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setFullName("Ada Analyst");
        user.setUsername("ada");
        user.setRole(role);
        return user;
    }
}
