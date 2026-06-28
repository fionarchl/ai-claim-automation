package com.insurance.claims.service.ai;

import com.insurance.claims.api.dto.ClaimDocumentRequest;
import com.insurance.claims.api.dto.RuleResultResponse;
import com.insurance.claims.domain.AiRecommendedDecision;
import com.insurance.claims.domain.AiProviderSettings;
import com.insurance.claims.domain.AiRuleSetting;
import com.insurance.claims.domain.Claim;
import com.insurance.claims.domain.Customer;
import com.insurance.claims.domain.Policy;
import com.insurance.claims.domain.PolicyBlacklistEntry;
import com.insurance.claims.domain.PolicyStatus;
import com.insurance.claims.domain.PolicyType;
import com.insurance.claims.repository.ClaimRepository;
import com.insurance.claims.repository.PolicyBlacklistEntryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

class ClaimAiRuleEngineTest {

    @Test
    void blacklistedPolicyProducesRejectRecommendation() {
        Policy policy = policy();
        PolicyBlacklistEntry entry = new PolicyBlacklistEntry();
        entry.setPolicy(policy);
        entry.setReason("Fraud investigation hold");
        entry.setSeverity("HIGH");

        PolicyBlacklistEntryRepository blacklistRepository = mock(PolicyBlacklistEntryRepository.class);
        ClaimRepository claimRepository = mock(ClaimRepository.class);
        AiConfigurationService aiConfigurationService = configurationService();
        when(blacklistRepository.findByPolicyIdAndActiveTrue(2L)).thenReturn(Collections.singletonList(entry));
        when(claimRepository.findByPolicyId(2L)).thenReturn(Collections.emptyList());

        AiRuleEvaluation evaluation = new ClaimAiRuleEngine(blacklistRepository, claimRepository, aiConfigurationService)
                .evaluate(policy, extraction(), documents());

        assertEquals(AiRecommendedDecision.REJECT, evaluation.getRecommendedDecision());
        assertTrue(hasOutcome(evaluation, "POLICY_BLACKLIST", "FAIL"));
    }

    @Test
    void cleanClaimProducesApproveRecommendation() {
        Policy policy = policy();
        PolicyBlacklistEntryRepository blacklistRepository = mock(PolicyBlacklistEntryRepository.class);
        ClaimRepository claimRepository = mock(ClaimRepository.class);
        AiConfigurationService aiConfigurationService = configurationService();
        when(blacklistRepository.findByPolicyIdAndActiveTrue(2L)).thenReturn(Collections.emptyList());
        when(claimRepository.findByPolicyId(2L)).thenReturn(Collections.emptyList());

        AiRuleEvaluation evaluation = new ClaimAiRuleEngine(blacklistRepository, claimRepository, aiConfigurationService)
                .evaluate(policy, extraction(), documents());

        assertEquals(AiRecommendedDecision.APPROVE, evaluation.getRecommendedDecision());
    }

    @Test
    void duplicateClaimProducesManualReviewRecommendation() {
        Policy policy = policy();
        Claim duplicate = new Claim();
        duplicate.setPolicy(policy);
        duplicate.setClaimNumber("CLM-DUP");
        duplicate.setAdmissionDate(LocalDate.of(2026, 6, 10));
        duplicate.setDischargeDate(LocalDate.of(2026, 6, 12));
        duplicate.setEstimatedAmount(new BigDecimal("12000000"));

        PolicyBlacklistEntryRepository blacklistRepository = mock(PolicyBlacklistEntryRepository.class);
        ClaimRepository claimRepository = mock(ClaimRepository.class);
        AiConfigurationService aiConfigurationService = configurationService();
        when(blacklistRepository.findByPolicyIdAndActiveTrue(2L)).thenReturn(Collections.emptyList());
        when(claimRepository.findByPolicyId(2L)).thenReturn(Collections.singletonList(duplicate));

        AiRuleEvaluation evaluation = new ClaimAiRuleEngine(blacklistRepository, claimRepository, aiConfigurationService)
                .evaluate(policy, extraction(), documents());

        assertEquals(AiRecommendedDecision.MANUAL_REVIEW, evaluation.getRecommendedDecision());
        assertTrue(hasOutcome(evaluation, "DUPLICATE_CLAIM", "WARN"));
    }

    private boolean hasOutcome(AiRuleEvaluation evaluation, String code, String outcome) {
        for (RuleResultResponse result : evaluation.getRuleResults()) {
            if (code.equals(result.getCode()) && outcome.equals(result.getOutcome())) {
                return true;
            }
        }
        return false;
    }

    private Policy policy() {
        Customer customer = new Customer();
        customer.setId(3L);
        customer.setFullName("Andi Wijaya");

        Policy policy = new Policy();
        policy.setId(2L);
        policy.setCustomer(customer);
        policy.setPolicyNumber("POL-HEALTH-0001");
        policy.setType(PolicyType.HEALTH);
        policy.setCoverageAmount(new BigDecimal("100000000"));
        policy.setStartDate(LocalDate.of(2026, 1, 1));
        policy.setEndDate(LocalDate.of(2026, 12, 31));
        policy.setStatus(PolicyStatus.ACTIVE);
        return policy;
    }

    private AiClaimExtraction extraction() {
        AiClaimExtraction extraction = new AiClaimExtraction();
        extraction.setPolicyNumber("POL-HEALTH-0001");
        extraction.setAdmissionDate(LocalDate.of(2026, 6, 10));
        extraction.setDischargeDate(LocalDate.of(2026, 6, 12));
        extraction.setEstimatedAmount(new BigDecimal("12000000"));
        extraction.setDescription("Hospitalization claim");
        extraction.setConfidence(new BigDecimal("0.86"));
        return extraction;
    }

    private java.util.List<ClaimDocumentRequest> documents() {
        ClaimDocumentRequest document = new ClaimDocumentRequest();
        document.setFileName("hospital-invoice.pdf");
        document.setContentType("application/pdf");
        document.setDataBase64("ZHVtbXk=");
        return Arrays.asList(document);
    }

    private AiConfigurationService configurationService() {
        AiConfigurationService service = mock(AiConfigurationService.class);
        AiProviderSettings settings = new AiProviderSettings();
        settings.setConfidenceThreshold(new BigDecimal("0.65"));
        when(service.getProviderSettings()).thenReturn(settings);
        when(service.rule(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            AiRuleDefaults.Definition definition = AiRuleDefaults.definition(code);
            AiRuleSetting setting = new AiRuleSetting();
            setting.setCode(code);
            setting.setLabel(definition.getLabel());
            setting.setEnabled(true);
            setting.setFailureOutcome(definition.getFailureOutcome());
            return setting;
        });
        return service;
    }
}
