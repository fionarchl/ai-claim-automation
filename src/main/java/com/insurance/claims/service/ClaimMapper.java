package com.insurance.claims.service;

import com.insurance.claims.api.dto.ClaimDetailResponse;
import com.insurance.claims.api.dto.ClaimDocumentResponse;
import com.insurance.claims.api.dto.ClaimNoteResponse;
import com.insurance.claims.api.dto.ClaimResponse;
import com.insurance.claims.api.dto.ClaimStatusHistoryResponse;
import com.insurance.claims.domain.Claim;
import com.insurance.claims.domain.ClaimDetail;
import com.insurance.claims.domain.ClaimDocument;
import com.insurance.claims.domain.ClaimNote;
import com.insurance.claims.domain.ClaimStatusHistory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ClaimMapper {
    private final ClaimDocumentService claimDocumentService;
    private final ClaimAmountService claimAmountService;

    public ClaimMapper(ClaimDocumentService claimDocumentService, ClaimAmountService claimAmountService) {
        this.claimDocumentService = claimDocumentService;
        this.claimAmountService = claimAmountService;
    }

    public ClaimResponse toResponse(Claim claim) {
        ClaimResponse response = new ClaimResponse();
        response.setId(claim.getId());
        response.setClaimNumber(claim.getClaimNumber());
        response.setPolicyId(claim.getPolicy().getId());
        response.setPolicyNumber(claim.getPolicy().getPolicyNumber());
        response.setPolicyCoverageAmount(claim.getPolicy().getCoverageAmount());
        response.setCustomerId(claim.getPolicy().getCustomer().getId());
        response.setCustomerName(claim.getPolicy().getCustomer().getFullName());
        response.setIncidentDate(claim.getIncidentDate());
        response.setAdmissionDate(claim.getAdmissionDate() != null ? claim.getAdmissionDate() : claim.getIncidentDate());
        response.setDischargeDate(claim.getDischargeDate() != null ? claim.getDischargeDate() : claim.getIncidentDate());
        response.setFiledDate(claim.getFiledDate());
        response.setDescription(claim.getDescription());
        response.setDocumentNames(toDocumentNames(claim.getClaimDocuments()));
        response.setDocuments(claimDocumentService.findByClaimId(claim.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
        response.setEstimatedAmount(claim.getEstimatedAmount());
        response.setApprovedAmount(claimAmountService.totalApprovedDetailAmount(claim.getId()));
        response.setStatus(claim.getStatus());
        response.setCreatedAt(claim.getCreatedAt());
        response.setUpdatedAt(claim.getUpdatedAt());
        return response;
    }

    public ClaimNoteResponse toResponse(ClaimNote note) {
        ClaimNoteResponse response = new ClaimNoteResponse();
        response.setId(note.getId());
        response.setClaimId(note.getClaim().getId());
        response.setAuthor(note.getAuthor());
        response.setMessage(note.getMessage());
        response.setCreatedAt(note.getCreatedAt());
        return response;
    }

    public ClaimDocumentResponse toResponse(ClaimDocument document) {
        ClaimDocumentResponse response = new ClaimDocumentResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setContentType(document.getContentType());
        response.setSize(document.getData() == null ? 0 : document.getData().length);
        response.setUploadedAt(document.getUploadedAt());
        return response;
    }

    public ClaimDetailResponse toResponse(ClaimDetail detail) {
        ClaimDetailResponse response = new ClaimDetailResponse();
        response.setId(detail.getId());
        response.setClaimId(detail.getClaim().getId());
        response.setCategory(detail.getCategory());
        response.setEventStartDate(detail.getEventStartDate());
        response.setEventEndDate(detail.getEventEndDate());
        response.setSubmittedAmount(detail.getSubmittedAmount());
        response.setApprovedAmount(detail.getApprovedAmount());
        response.setRejectedAmount(detail.getRejectedAmount());
        response.setDescription(detail.getDescription());
        response.setCreatedAt(detail.getCreatedAt());
        response.setUpdatedAt(detail.getUpdatedAt());
        return response;
    }

    public ClaimStatusHistoryResponse toResponse(ClaimStatusHistory history) {
        ClaimStatusHistoryResponse response = new ClaimStatusHistoryResponse();
        response.setId(history.getId());
        response.setClaimId(history.getClaim().getId());
        response.setFromStatus(history.getFromStatus());
        response.setToStatus(history.getToStatus());
        response.setChangedBy(history.getChangedBy());
        response.setComment(history.getComment());
        response.setChangedAt(history.getChangedAt());
        return response;
    }

    private List<String> toDocumentNames(String documentNames) {
        if (documentNames == null || documentNames.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(documentNames.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }
}
