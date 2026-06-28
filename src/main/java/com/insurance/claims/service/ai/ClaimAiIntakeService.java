package com.insurance.claims.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.claims.api.dto.AiClaimIntakeRequest;
import com.insurance.claims.api.dto.AiClaimIntakeResponse;
import com.insurance.claims.api.dto.ClaimAiAssessmentResponse;
import com.insurance.claims.api.dto.RuleResultResponse;
import com.insurance.claims.domain.Claim;
import com.insurance.claims.domain.ClaimAiAssessment;
import com.insurance.claims.domain.Policy;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.exception.BadRequestException;
import com.insurance.claims.exception.ForbiddenException;
import com.insurance.claims.exception.ResourceNotFoundException;
import com.insurance.claims.repository.ClaimAiAssessmentRepository;
import com.insurance.claims.repository.PolicyRepository;
import com.insurance.claims.service.ClaimMapper;
import com.insurance.claims.service.ClaimService;
import com.insurance.claims.service.RolePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ClaimAiIntakeService {
    private static final String PROMPT_VERSION = "claim-ai-intake-v1";

    private final AiClaimExtractionService aiClaimExtractionService;
    private final AiClaimReasoningService aiClaimReasoningService;
    private final ClaimAiRuleEngine claimAiRuleEngine;
    private final ClaimService claimService;
    private final ClaimMapper claimMapper;
    private final PolicyRepository policyRepository;
    private final ClaimAiAssessmentRepository claimAiAssessmentRepository;
    private final ObjectMapper objectMapper;

    public ClaimAiIntakeService(AiClaimExtractionService aiClaimExtractionService,
                                AiClaimReasoningService aiClaimReasoningService,
                                ClaimAiRuleEngine claimAiRuleEngine,
                                ClaimService claimService,
                                ClaimMapper claimMapper,
                                PolicyRepository policyRepository,
                                ClaimAiAssessmentRepository claimAiAssessmentRepository,
                                ObjectMapper objectMapper) {
        this.aiClaimExtractionService = aiClaimExtractionService;
        this.aiClaimReasoningService = aiClaimReasoningService;
        this.claimAiRuleEngine = claimAiRuleEngine;
        this.claimService = claimService;
        this.claimMapper = claimMapper;
        this.policyRepository = policyRepository;
        this.claimAiAssessmentRepository = claimAiAssessmentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiClaimIntakeResponse register(AiClaimIntakeRequest request, SystemUser user) {
        AiClaimExtraction extraction = aiClaimExtractionService.extract(request);
        Policy policy = resolvePolicy(request, extraction);
        AiRuleEvaluation evaluation = claimAiRuleEngine.evaluate(policy, extraction, request.getDocuments());
        AiReasoningResult reasoning = aiClaimReasoningService.reason(extraction, policy, evaluation);
        Claim claim = claimService.createAiRegisteredClaim(policy, extraction, request.getDocuments(), user);

        ClaimAiAssessment assessment = new ClaimAiAssessment();
        assessment.markCreatedBy(actorName(user));
        assessment.setClaim(claim);
        assessment.setRecommendedDecision(evaluation.getRecommendedDecision());
        assessment.setConfidenceScore(normalizeConfidence(extraction.getConfidence()));
        assessment.setSummary(firstNonBlank(reasoning.getSummary(), evaluation.getSummary()));
        assessment.setExtractedDataJson(toJson(extraction));
        assessment.setRuleResultsJson(toJson(evaluation.getRuleResults()));
        assessment.setEvidenceJson(toJson(evidence(extraction, reasoning)));
        assessment.setModelName(aiClaimExtractionService.modelName() + " / " + reasoning.getModelName());
        assessment.setPromptVersion(PROMPT_VERSION);
        assessment.setProcessedAt(LocalDateTime.now());
        ClaimAiAssessment savedAssessment = claimAiAssessmentRepository.save(assessment);

        AiClaimIntakeResponse response = new AiClaimIntakeResponse();
        response.setClaim(claimMapper.toResponse(claim));
        response.setAssessment(toResponse(savedAssessment));
        return response;
    }

    @Transactional(readOnly = true)
    public ClaimAiAssessmentResponse findLatestAssessment(Long claimId, SystemUser user) {
        Claim claim = claimService.getClaim(claimId);
        if (!RolePolicy.canView(user, claim.getStatus())) {
            throw new ForbiddenException("You are not allowed to view this claim status");
        }
        ClaimAiAssessment assessment = claimAiAssessmentRepository.findTopByClaimIdOrderByProcessedAtDesc(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("AI assessment not found for claim: " + claimId));
        return toResponse(assessment);
    }

    private Policy resolvePolicy(AiClaimIntakeRequest request, AiClaimExtraction extraction) {
        if (request.getPolicyId() != null) {
            return policyRepository.findById(request.getPolicyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + request.getPolicyId()));
        }
        String policyNumber = firstNonBlank(request.getPolicyNumber(), extraction.getPolicyNumber());
        if (policyNumber == null) {
            throw new BadRequestException("AI could not identify a policy number. Select a policy before registering via AI.");
        }
        return policyRepository.findByPolicyNumber(policyNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyNumber));
    }

    private ClaimAiAssessmentResponse toResponse(ClaimAiAssessment assessment) {
        ClaimAiAssessmentResponse response = new ClaimAiAssessmentResponse();
        response.setId(assessment.getId());
        response.setClaimId(assessment.getClaim().getId());
        response.setRecommendedDecision(assessment.getRecommendedDecision());
        response.setConfidenceScore(assessment.getConfidenceScore());
        response.setSummary(assessment.getSummary());
        response.setModelName(assessment.getModelName());
        response.setPromptVersion(assessment.getPromptVersion());
        response.setProcessedAt(assessment.getProcessedAt());
        response.setRuleResults(readRuleResults(assessment.getRuleResultsJson()));
        response.setEvidence(readStringList(assessment.getEvidenceJson()));
        return response;
    }

    private List<RuleResultResponse> readRuleResults(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<RuleResultResponse>>() {
            });
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private List<String> evidence(AiClaimExtraction extraction, AiReasoningResult reasoning) {
        List<String> evidence = new ArrayList<String>();
        if (extraction.getEvidence() != null) {
            evidence.addAll(extraction.getEvidence());
        }
        if (reasoning.getEvidence() != null) {
            evidence.addAll(reasoning.getEvidence());
        }
        if (extraction.getRiskIndicators() != null) {
            for (String risk : extraction.getRiskIndicators()) {
                evidence.add("Risk indicator: " + risk);
            }
        }
        return evidence;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Could not store AI assessment JSON: " + ex.getOriginalMessage());
        }
    }

    private BigDecimal normalizeConfidence(BigDecimal confidence) {
        if (confidence == null) {
            return BigDecimal.ZERO;
        }
        if (confidence.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (confidence.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return confidence;
    }

    private String actorName(SystemUser user) {
        if (user == null) {
            return "system";
        }
        return user.getFullName() != null && !user.getFullName().trim().isEmpty()
                ? user.getFullName()
                : user.getUsername();
    }
}
