package com.insurance.claims.service;

import com.insurance.claims.api.dto.ClaimDocumentRequest;
import com.insurance.claims.domain.Claim;
import com.insurance.claims.domain.ClaimDocument;
import com.insurance.claims.exception.BadRequestException;
import com.insurance.claims.exception.ResourceNotFoundException;
import com.insurance.claims.repository.ClaimDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

@Service
public class ClaimDocumentService {
    private final ClaimDocumentRepository claimDocumentRepository;

    public ClaimDocumentService(ClaimDocumentRepository claimDocumentRepository) {
        this.claimDocumentRepository = claimDocumentRepository;
    }

    public void saveDocuments(Claim claim, List<ClaimDocumentRequest> documents, String actor) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        for (ClaimDocumentRequest request : documents) {
            ClaimDocument document = new ClaimDocument();
            document.markCreatedBy(actor);
            document.setClaim(claim);
            document.setFileName(request.getFileName());
            document.setContentType(request.getContentType() == null || request.getContentType().trim().isEmpty()
                    ? "application/octet-stream"
                    : request.getContentType().trim());
            try {
                document.setData(Base64.getDecoder().decode(request.getDataBase64()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Document dataBase64 is not valid Base64 for file: " + request.getFileName());
            }
            claimDocumentRepository.save(document);
        }
    }

    public List<ClaimDocument> findByClaimId(Long claimId) {
        return claimDocumentRepository.findByClaimIdOrderByCreatedAtDesc(claimId);
    }

    public ClaimDocument getDocument(Long claimId, Long documentId) {
        return claimDocumentRepository.findByIdAndClaimId(documentId, claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim document not found: " + documentId));
    }

    public void deleteDocument(Long claimId, Long documentId) {
        ClaimDocument document = getDocument(claimId, documentId);
        claimDocumentRepository.delete(document);
    }
}
