package com.insurance.claims.service;

import com.insurance.claims.api.dto.ClaimDetailRequest;
import com.insurance.claims.api.dto.ClaimDetailResponse;
import com.insurance.claims.api.dto.ClaimDocumentRequest;
import com.insurance.claims.api.dto.ClaimNoteRequest;
import com.insurance.claims.api.dto.ClaimNoteResponse;
import com.insurance.claims.api.dto.ClaimRequest;
import com.insurance.claims.api.dto.ClaimResponse;
import com.insurance.claims.api.dto.ClaimStatusHistoryResponse;
import com.insurance.claims.api.dto.ClaimStatusUpdateRequest;
import com.insurance.claims.api.dto.ClaimUpdateRequest;
import com.insurance.claims.domain.Claim;
import com.insurance.claims.domain.ClaimDetail;
import com.insurance.claims.domain.ClaimDocument;
import com.insurance.claims.domain.ClaimNote;
import com.insurance.claims.domain.ClaimStatus;
import com.insurance.claims.domain.ClaimStatusHistory;
import com.insurance.claims.domain.Policy;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.exception.BadRequestException;
import com.insurance.claims.exception.ForbiddenException;
import com.insurance.claims.exception.ResourceNotFoundException;
import com.insurance.claims.repository.ClaimDetailRepository;
import com.insurance.claims.repository.ClaimNoteRepository;
import com.insurance.claims.repository.ClaimRepository;
import com.insurance.claims.repository.ClaimStatusHistoryRepository;
import com.insurance.claims.service.ai.AiClaimDetailExtraction;
import com.insurance.claims.service.ai.AiClaimExtraction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class ClaimService {
    private static final DateTimeFormatter CLAIM_NUMBER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final ClaimRepository claimRepository;
    private final ClaimNoteRepository claimNoteRepository;
    private final ClaimDetailRepository claimDetailRepository;
    private final ClaimStatusHistoryRepository claimStatusHistoryRepository;
    private final PolicyService policyService;
    private final ClaimWorkflowPolicy claimWorkflowPolicy;
    private final ClaimDocumentService claimDocumentService;
    private final ClaimAmountService claimAmountService;
    private final ClaimMapper claimMapper;

    public ClaimService(ClaimRepository claimRepository,
                        ClaimNoteRepository claimNoteRepository,
                        ClaimDetailRepository claimDetailRepository,
                        ClaimStatusHistoryRepository claimStatusHistoryRepository,
                        PolicyService policyService,
                        ClaimWorkflowPolicy claimWorkflowPolicy,
                        ClaimDocumentService claimDocumentService,
                        ClaimAmountService claimAmountService,
                        ClaimMapper claimMapper) {
        this.claimRepository = claimRepository;
        this.claimNoteRepository = claimNoteRepository;
        this.claimDetailRepository = claimDetailRepository;
        this.claimStatusHistoryRepository = claimStatusHistoryRepository;
        this.policyService = policyService;
        this.claimWorkflowPolicy = claimWorkflowPolicy;
        this.claimDocumentService = claimDocumentService;
        this.claimAmountService = claimAmountService;
        this.claimMapper = claimMapper;
    }

    @Transactional
    public ClaimResponse create(ClaimRequest request, SystemUser user) {
        if (!RolePolicy.canCreate(user)) {
            throw new ForbiddenException("You are not allowed to create claims");
        }

        Policy policy = policyService.getPolicy(request.getPolicyId());
        claimWorkflowPolicy.validatePolicyCanReceiveClaim(policy, request.getAdmissionDate(), request.getDischargeDate());

        Claim claim = new Claim();
        claim.markCreatedBy(actorName(user));
        claim.setPolicy(policy);
        claim.setClaimNumber(generateClaimNumber());
        claim.setIncidentDate(request.getAdmissionDate());
        claim.setAdmissionDate(request.getAdmissionDate());
        claim.setDischargeDate(request.getDischargeDate());
        claim.setDescription(request.getDescription());
        claim.setClaimDocuments(toDocumentString(request.getDocumentNames()));
        claim.setEstimatedAmount(request.getEstimatedAmount());
        claim.setStatus(ClaimStatus.FILED);

        Claim saved = claimRepository.save(claim);
        claimDocumentService.saveDocuments(saved, request.getDocuments(), actorName(user));
        recordStatusChange(saved, null, ClaimStatus.FILED, actorName(user), "Claim filed");
        return claimMapper.toResponse(saved);
    }

    @Transactional
    public Claim createAiRegisteredClaim(Policy policy,
                                         AiClaimExtraction extraction,
                                         List<ClaimDocumentRequest> documents,
                                         SystemUser user) {
        if (!RolePolicy.canCreate(user)) {
            throw new ForbiddenException("You are not allowed to create claims");
        }
        validateAiExtraction(extraction);

        Claim claim = new Claim();
        claim.markCreatedBy(actorName(user));
        claim.setPolicy(policy);
        claim.setClaimNumber(generateClaimNumber());
        claim.setIncidentDate(extraction.getAdmissionDate());
        claim.setAdmissionDate(extraction.getAdmissionDate());
        claim.setDischargeDate(extraction.getDischargeDate());
        claim.setDescription(extraction.getDescription());
        claim.setClaimDocuments(toDocumentString(documentNames(documents)));
        claim.setEstimatedAmount(extraction.getEstimatedAmount());
        claim.setStatus(ClaimStatus.UNDER_REVIEW);

        Claim saved = claimRepository.save(claim);
        claimDocumentService.saveDocuments(saved, documents, actorName(user));
        saveExtractedDetails(saved, extraction, actorName(user));
        recordStatusChange(saved, null, ClaimStatus.UNDER_REVIEW, actorName(user),
                "Registered via AI intake. Awaiting Claim Analyst review.");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> findAll(ClaimStatus status, Long policyId, SystemUser user) {
        List<Claim> claims;
        if (status != null && policyId != null) {
            claims = claimRepository.findByPolicyIdAndStatus(policyId, status);
        } else if (status != null) {
            claims = claimRepository.findByStatus(status);
        } else if (policyId != null) {
            claims = claimRepository.findByPolicyId(policyId);
        } else {
            claims = claimRepository.findAll();
        }

        return claims.stream()
                .filter(claim -> RolePolicy.canView(user, claim.getStatus()))
                .map(claimMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClaimResponse findById(Long id, SystemUser user) {
        Claim claim = getClaim(id);
        requireView(user, claim);
        return claimMapper.toResponse(claim);
    }

    @Transactional
    public ClaimResponse updateDetails(Long id, ClaimUpdateRequest request, SystemUser user) {
        Claim claim = getClaim(id);
        if (!RolePolicy.canEditDetails(user, claim.getStatus())) {
            throw new ForbiddenException("You are not allowed to edit this claim status");
        }
        claimWorkflowPolicy.validatePolicyCanReceiveClaim(claim.getPolicy(), request.getAdmissionDate(), request.getDischargeDate());
        claim.setIncidentDate(request.getAdmissionDate());
        claim.setAdmissionDate(request.getAdmissionDate());
        claim.setDischargeDate(request.getDischargeDate());
        claim.setDescription(request.getDescription());
        claim.setClaimDocuments(toDocumentString(request.getDocumentNames()));
        claim.setEstimatedAmount(request.getEstimatedAmount());
        claim.markUpdatedBy(actorName(user));
        Claim saved = claimRepository.save(claim);
        claimDocumentService.saveDocuments(saved, request.getDocuments(), actorName(user));
        return claimMapper.toResponse(saved);
    }

    @Transactional
    public ClaimResponse updateStatus(Long id, ClaimStatusUpdateRequest request, SystemUser user) {
        Claim claim = getClaim(id);
        ClaimStatus current = claim.getStatus();
        ClaimStatus requested = request.getStatus();
        requireView(user, claim);

        claimWorkflowPolicy.validateStatusTransition(current, requested);
        if (!RolePolicy.canMoveTo(user, requested)) {
            throw new ForbiddenException("Your role cannot move claims to " + requested);
        }
        if (requested == ClaimStatus.APPROVED || requested == ClaimStatus.PAID) {
            BigDecimal approvedTotal = claimAmountService.totalApprovedDetailAmount(claim.getId());
            if (approvedTotal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException(requested == ClaimStatus.APPROVED
                        ? "At least one approved claim detail amount is required when approving a claim"
                        : "Claim must have an approved amount before payment");
            }
            claimWorkflowPolicy.validateApprovedAmount(claim, approvedTotal);
            claim.setApprovedAmount(approvedTotal);
        }

        claim.setStatus(requested);
        claim.markUpdatedBy(actorName(user));
        Claim saved = claimRepository.save(claim);
        recordStatusChange(saved, current, requested, request.getChangedBy(), request.getComment());
        return claimMapper.toResponse(saved);
    }

    @Transactional
    public ClaimNoteResponse addNote(Long claimId, ClaimNoteRequest request, SystemUser user) {
        Claim claim = getClaim(claimId);
        if (!RolePolicy.canAddNote(user, claim.getStatus())) {
            throw new ForbiddenException("You are not allowed to add notes to this claim");
        }
        ClaimNote note = new ClaimNote();
        note.markCreatedBy(actorName(user));
        note.setClaim(claim);
        note.setAuthor(request.getAuthor());
        note.setMessage(request.getMessage());
        return claimMapper.toResponse(claimNoteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public List<ClaimNoteResponse> findNotes(Long claimId, SystemUser user) {
        Claim claim = getClaim(claimId);
        requireView(user, claim);
        return claimNoteRepository.findByClaimIdOrderByCreatedAtDesc(claimId)
                .stream()
                .map(claimMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClaimStatusHistoryResponse> findHistory(Long claimId, SystemUser user) {
        Claim claim = getClaim(claimId);
        requireView(user, claim);
        return claimStatusHistoryRepository.findByClaimIdOrderByCreatedAtDesc(claimId)
                .stream()
                .map(claimMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClaimDetailResponse> findDetails(Long claimId, SystemUser user) {
        Claim claim = getClaim(claimId);
        requireView(user, claim);
        return claimDetailRepository.findByClaimIdOrderByCreatedAtDesc(claimId)
                .stream()
                .map(claimMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClaimDetailResponse addDetail(Long claimId, ClaimDetailRequest request, SystemUser user) {
        Claim claim = getClaim(claimId);
        if (!RolePolicy.canEditDetails(user, claim.getStatus())) {
            throw new ForbiddenException("You are not allowed to add claim details to this claim");
        }
        claimWorkflowPolicy.validateClaimDetail(claim, request.getEventStartDate(), request.getEventEndDate(),
                request.getSubmittedAmount(), request.getApprovedAmount());

        ClaimDetail detail = new ClaimDetail();
        detail.markCreatedBy(actorName(user));
        detail.setClaim(claim);
        applyDetailRequest(detail, request);
        ClaimDetail saved = claimDetailRepository.save(detail);
        claimAmountService.syncClaimApprovedAmount(claim, actorName(user));
        return claimMapper.toResponse(saved);
    }

    @Transactional
    public ClaimDetailResponse updateDetail(Long claimId, Long detailId, ClaimDetailRequest request, SystemUser user) {
        Claim claim = getClaim(claimId);
        if (!RolePolicy.canEditDetails(user, claim.getStatus())) {
            throw new ForbiddenException("You are not allowed to edit claim details for this claim");
        }
        claimWorkflowPolicy.validateClaimDetail(claim, request.getEventStartDate(), request.getEventEndDate(),
                request.getSubmittedAmount(), request.getApprovedAmount());

        ClaimDetail detail = getClaimDetail(claimId, detailId);
        applyDetailRequest(detail, request);
        detail.markUpdatedBy(actorName(user));
        ClaimDetail saved = claimDetailRepository.save(detail);
        claimAmountService.syncClaimApprovedAmount(claim, actorName(user));
        return claimMapper.toResponse(saved);
    }

    @Transactional
    public void deleteDetail(Long claimId, Long detailId, SystemUser user) {
        Claim claim = getClaim(claimId);
        if (!RolePolicy.canEditDetails(user, claim.getStatus())) {
            throw new ForbiddenException("You are not allowed to delete claim details for this claim");
        }
        ClaimDetail detail = getClaimDetail(claimId, detailId);
        claimDetailRepository.delete(detail);
        claimAmountService.syncClaimApprovedAmount(claim, actorName(user));
    }

    @Transactional(readOnly = true)
    public ClaimDocument getDocument(Long claimId, Long documentId, SystemUser user) {
        Claim claim = getClaim(claimId);
        requireView(user, claim);
        return claimDocumentService.getDocument(claimId, documentId);
    }

    @Transactional
    public void deleteDocument(Long claimId, Long documentId, SystemUser user) {
        Claim claim = getClaim(claimId);
        if (!RolePolicy.canEditDetails(user, claim.getStatus())) {
            throw new ForbiddenException("You are not allowed to delete documents for this claim");
        }
        claimDocumentService.deleteDocument(claimId, documentId);
        claim.markUpdatedBy(actorName(user));
        claimRepository.save(claim);
    }

    @Transactional(readOnly = true)
    public Claim getClaim(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + id));
    }

    private ClaimDetail getClaimDetail(Long claimId, Long detailId) {
        return claimDetailRepository.findByIdAndClaimId(detailId, claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim detail not found: " + detailId));
    }

    private void applyDetailRequest(ClaimDetail detail, ClaimDetailRequest request) {
        detail.setCategory(request.getCategory());
        detail.setEventStartDate(request.getEventStartDate());
        detail.setEventEndDate(request.getEventEndDate());
        detail.setSubmittedAmount(request.getSubmittedAmount());
        detail.setApprovedAmount(request.getApprovedAmount());
        detail.setRejectedAmount(calculateRejectedAmount(request.getSubmittedAmount(), request.getApprovedAmount()));
        detail.setDescription(request.getDescription());
    }

    private BigDecimal calculateRejectedAmount(BigDecimal submittedAmount, BigDecimal approvedAmount) {
        if (approvedAmount == null) {
            return null;
        }
        return submittedAmount.subtract(approvedAmount);
    }

    private String actorName(SystemUser user) {
        if (user == null) {
            return "system";
        }
        return user.getFullName() != null && !user.getFullName().trim().isEmpty()
                ? user.getFullName()
                : user.getUsername();
    }

    private void requireView(SystemUser user, Claim claim) {
        if (!RolePolicy.canView(user, claim.getStatus())) {
            throw new ForbiddenException("You are not allowed to view this claim status");
        }
    }

    private String generateClaimNumber() {
        String prefix = "CLM-" + CLAIM_NUMBER_DATE.format(LocalDate.now()) + "-";
        String claimNumber;
        do {
            claimNumber = prefix + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        } while (claimRepository.existsByClaimNumber(claimNumber));
        return claimNumber;
    }

    private void validateAiExtraction(AiClaimExtraction extraction) {
        if (extraction.getAdmissionDate() == null) {
            throw new BadRequestException("AI could not extract admission date");
        }
        if (extraction.getDischargeDate() == null) {
            throw new BadRequestException("AI could not extract discharge date");
        }
        if (extraction.getEstimatedAmount() == null || extraction.getEstimatedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("AI could not extract a positive estimated amount");
        }
        if (extraction.getDescription() == null || extraction.getDescription().trim().isEmpty()) {
            throw new BadRequestException("AI could not extract a claim description");
        }
    }

    private void saveExtractedDetails(Claim claim, AiClaimExtraction extraction, String actor) {
        if (extraction.getClaimDetails() == null) {
            return;
        }
        for (AiClaimDetailExtraction extractedDetail : extraction.getClaimDetails()) {
            if (extractedDetail.getCategory() == null
                    || extractedDetail.getEventStartDate() == null
                    || extractedDetail.getEventEndDate() == null
                    || extractedDetail.getSubmittedAmount() == null) {
                continue;
            }
            ClaimDetail detail = new ClaimDetail();
            detail.markCreatedBy(actor);
            detail.setClaim(claim);
            detail.setCategory(extractedDetail.getCategory());
            detail.setEventStartDate(extractedDetail.getEventStartDate());
            detail.setEventEndDate(extractedDetail.getEventEndDate());
            detail.setSubmittedAmount(extractedDetail.getSubmittedAmount());
            detail.setApprovedAmount(null);
            detail.setRejectedAmount(null);
            detail.setDescription(extractedDetail.getDescription());
            claimDetailRepository.save(detail);
        }
    }

    private List<String> documentNames(List<ClaimDocumentRequest> documents) {
        if (documents == null) {
            return null;
        }
        return documents.stream()
                .map(ClaimDocumentRequest::getFileName)
                .collect(Collectors.toList());
    }

    private String toDocumentString(List<String> documentNames) {
        if (documentNames == null || documentNames.isEmpty()) {
            return "";
        }
        return documentNames.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    private void recordStatusChange(Claim claim, ClaimStatus from, ClaimStatus to, String changedBy, String comment) {
        ClaimStatusHistory history = new ClaimStatusHistory();
        history.markCreatedBy(changedBy);
        history.setClaim(claim);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(changedBy);
        history.setComment(comment);
        claimStatusHistoryRepository.save(history);
    }
}
