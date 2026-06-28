package com.insurance.claims.service.ai;

import com.insurance.claims.api.dto.ClaimDocumentRequest;
import com.insurance.claims.api.dto.RuleResultResponse;
import com.insurance.claims.domain.AiRecommendedDecision;
import com.insurance.claims.domain.AiRuleSetting;
import com.insurance.claims.domain.Claim;
import com.insurance.claims.domain.Policy;
import com.insurance.claims.domain.PolicyBlacklistEntry;
import com.insurance.claims.domain.PolicyStatus;
import com.insurance.claims.repository.ClaimRepository;
import com.insurance.claims.repository.PolicyBlacklistEntryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ClaimAiRuleEngine {
    private final PolicyBlacklistEntryRepository policyBlacklistEntryRepository;
    private final ClaimRepository claimRepository;
    private final AiConfigurationService aiConfigurationService;

    public ClaimAiRuleEngine(PolicyBlacklistEntryRepository policyBlacklistEntryRepository,
                             ClaimRepository claimRepository,
                             AiConfigurationService aiConfigurationService) {
        this.policyBlacklistEntryRepository = policyBlacklistEntryRepository;
        this.claimRepository = claimRepository;
        this.aiConfigurationService = aiConfigurationService;
    }

    public AiRuleEvaluation evaluate(Policy policy,
                                     AiClaimExtraction extraction,
                                     List<ClaimDocumentRequest> documents) {
        AiRuleEvaluation evaluation = new AiRuleEvaluation();
        addPolicyActiveRule(evaluation, policy);
        addPolicyBlacklistRule(evaluation, policy);
        addDateRules(evaluation, policy, extraction);
        addCoverageRule(evaluation, policy, extraction);
        addDocumentRule(evaluation, documents);
        addDuplicateRule(evaluation, policy, extraction);
        addConfidenceRule(evaluation, extraction);

        boolean hasFail = false;
        boolean hasWarn = false;
        for (RuleResultResponse result : evaluation.getRuleResults()) {
            hasFail = hasFail || "FAIL".equals(result.getOutcome());
            hasWarn = hasWarn || "WARN".equals(result.getOutcome());
        }

        if (hasFail) {
            evaluation.setRecommendedDecision(AiRecommendedDecision.REJECT);
            evaluation.setSummary("AI intake found blocking rule failures. Claim was registered under review for analyst validation.");
        } else if (hasWarn) {
            evaluation.setRecommendedDecision(AiRecommendedDecision.MANUAL_REVIEW);
            evaluation.setSummary("AI intake found review items that need analyst attention before a final decision.");
        } else {
            evaluation.setRecommendedDecision(AiRecommendedDecision.APPROVE);
            evaluation.setSummary("AI intake found no blocking issues. Analyst should verify extracted data before approval.");
        }
        return evaluation;
    }

    private void addPolicyActiveRule(AiRuleEvaluation evaluation, Policy policy) {
        if (policy.getStatus() == PolicyStatus.ACTIVE) {
            pass(evaluation, "POLICY_ACTIVE", "Policy active", "Policy is active.");
        } else {
            fail(evaluation, "POLICY_ACTIVE", "Policy active", "Policy status is " + policy.getStatus() + ".");
        }
    }

    private void addPolicyBlacklistRule(AiRuleEvaluation evaluation, Policy policy) {
        List<PolicyBlacklistEntry> entries = policyBlacklistEntryRepository.findByPolicyIdAndActiveTrue(policy.getId());
        if (entries.isEmpty()) {
            pass(evaluation, "POLICY_BLACKLIST", "Policy blacklist", "Policy has no active blacklist entry.");
            return;
        }
        PolicyBlacklistEntry entry = entries.get(0);
        fail(evaluation, "POLICY_BLACKLIST", "Policy blacklist",
                "Policy is blacklisted: " + entry.getReason() + " (" + entry.getSeverity() + ").");
    }

    private void addDateRules(AiRuleEvaluation evaluation, Policy policy, AiClaimExtraction extraction) {
        if (extraction.getAdmissionDate() == null || extraction.getDischargeDate() == null) {
            fail(evaluation, "CLAIM_DATES_PRESENT", "Claim dates present", "AI could not extract complete admission/discharge dates.");
            return;
        }
        pass(evaluation, "CLAIM_DATES_PRESENT", "Claim dates present", "Admission and discharge dates were extracted.");
        if (extraction.getDischargeDate().isBefore(extraction.getAdmissionDate())) {
            fail(evaluation, "CLAIM_DATE_ORDER", "Claim date order", "Discharge date is before admission date.");
        } else {
            pass(evaluation, "CLAIM_DATE_ORDER", "Claim date order", "Discharge date is not before admission date.");
        }
        if (extraction.getAdmissionDate().isBefore(policy.getStartDate()) || extraction.getDischargeDate().isAfter(policy.getEndDate())) {
            fail(evaluation, "POLICY_COVERAGE_DATES", "Coverage dates", "Claim dates fall outside policy coverage period.");
        } else {
            pass(evaluation, "POLICY_COVERAGE_DATES", "Coverage dates", "Claim dates are inside policy coverage period.");
        }
    }

    private void addCoverageRule(AiRuleEvaluation evaluation, Policy policy, AiClaimExtraction extraction) {
        BigDecimal estimated = extraction.getEstimatedAmount();
        if (estimated == null || estimated.compareTo(BigDecimal.ZERO) <= 0) {
            fail(evaluation, "CLAIM_AMOUNT_PRESENT", "Claim amount present", "AI could not extract a positive estimated amount.");
            return;
        }
        pass(evaluation, "CLAIM_AMOUNT_PRESENT", "Claim amount present", "AI extracted a positive estimated amount.");
        if (estimated.compareTo(policy.getCoverageAmount()) > 0) {
            fail(evaluation, "COVERAGE_LIMIT", "Coverage limit", "Estimated amount exceeds policy coverage.");
        } else {
            pass(evaluation, "COVERAGE_LIMIT", "Coverage limit", "Estimated amount is within policy coverage.");
        }
    }

    private void addDocumentRule(AiRuleEvaluation evaluation, List<ClaimDocumentRequest> documents) {
        if (documents == null || documents.isEmpty()) {
            fail(evaluation, "DOCUMENTS_PRESENT", "Required documents", "No claim documents were uploaded.");
            return;
        }
        pass(evaluation, "DOCUMENTS_PRESENT", "Required documents", "At least one claim document was uploaded.");

        boolean hasBillingDocument = false;
        for (ClaimDocumentRequest document : documents) {
            String name = document.getFileName() == null ? "" : document.getFileName().toLowerCase();
            hasBillingDocument = hasBillingDocument
                    || name.contains("invoice")
                    || name.contains("receipt")
                    || name.contains("bill")
                    || name.contains("kwitansi");
        }
        if (hasBillingDocument) {
            pass(evaluation, "BILLING_DOCUMENT", "Billing evidence", "Uploaded documents include likely billing evidence.");
        } else {
            fail(evaluation, "BILLING_DOCUMENT", "Billing evidence", "No obvious invoice/receipt/bill document was detected.");
        }
    }

    private void addDuplicateRule(AiRuleEvaluation evaluation, Policy policy, AiClaimExtraction extraction) {
        List<Claim> claims = claimRepository.findByPolicyId(policy.getId());
        for (Claim claim : claims) {
            boolean sameDates = extraction.getAdmissionDate() != null
                    && extraction.getDischargeDate() != null
                    && extraction.getAdmissionDate().equals(claim.getAdmissionDate())
                    && extraction.getDischargeDate().equals(claim.getDischargeDate());
            boolean sameAmount = extraction.getEstimatedAmount() != null
                    && extraction.getEstimatedAmount().compareTo(claim.getEstimatedAmount()) == 0;
            if (sameDates && sameAmount) {
                fail(evaluation, "DUPLICATE_CLAIM", "Duplicate claim", "Similar claim already exists: " + claim.getClaimNumber() + ".");
                return;
            }
        }
        pass(evaluation, "DUPLICATE_CLAIM", "Duplicate claim", "No same-policy claim with matching dates and amount was found.");
    }

    private void addConfidenceRule(AiRuleEvaluation evaluation, AiClaimExtraction extraction) {
        if (extraction.getConfidence() == null) {
            fail(evaluation, "AI_CONFIDENCE", "AI confidence", "AI provider did not return a confidence score.");
            return;
        }
        BigDecimal threshold = aiConfigurationService.getProviderSettings().getConfidenceThreshold();
        if (extraction.getConfidence().compareTo(threshold) < 0) {
            fail(evaluation, "AI_CONFIDENCE", "AI confidence", "AI confidence is below the review threshold.");
        } else {
            pass(evaluation, "AI_CONFIDENCE", "AI confidence", "AI confidence meets the review threshold.");
        }
    }

    private void pass(AiRuleEvaluation evaluation, String code, String label, String message) {
        AiRuleSetting setting = aiConfigurationService.rule(code);
        if (!setting.isEnabled()) {
            evaluation.getRuleResults().add(new RuleResultResponse(code, label, "SKIP", message));
            return;
        }
        evaluation.getRuleResults().add(new RuleResultResponse(code, label, "PASS", message));
    }

    private void warn(AiRuleEvaluation evaluation, String code, String label, String message) {
        AiRuleSetting setting = aiConfigurationService.rule(code);
        if (!setting.isEnabled() || AiRuleDefaults.SKIP.equals(setting.getFailureOutcome())) {
            evaluation.getRuleResults().add(new RuleResultResponse(code, label, "SKIP", message));
            return;
        }
        evaluation.getRuleResults().add(new RuleResultResponse(code, label, "WARN", message));
    }

    private void fail(AiRuleEvaluation evaluation, String code, String label, String message) {
        AiRuleSetting setting = aiConfigurationService.rule(code);
        if (!setting.isEnabled() || AiRuleDefaults.SKIP.equals(setting.getFailureOutcome())) {
            evaluation.getRuleResults().add(new RuleResultResponse(code, label, "SKIP", message));
            return;
        }
        evaluation.getRuleResults().add(new RuleResultResponse(code, label, setting.getFailureOutcome(), message));
    }
}
