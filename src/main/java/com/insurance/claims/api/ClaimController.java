package com.insurance.claims.api;

import com.insurance.claims.api.dto.AiClaimIntakeRequest;
import com.insurance.claims.api.dto.AiClaimIntakeResponse;
import com.insurance.claims.api.dto.ClaimAiAssessmentResponse;
import com.insurance.claims.api.dto.ClaimNoteRequest;
import com.insurance.claims.api.dto.ClaimNoteResponse;
import com.insurance.claims.api.dto.ClaimDetailRequest;
import com.insurance.claims.api.dto.ClaimDetailResponse;
import com.insurance.claims.api.dto.ClaimRequest;
import com.insurance.claims.api.dto.ClaimResponse;
import com.insurance.claims.api.dto.ClaimStatusHistoryResponse;
import com.insurance.claims.api.dto.ClaimStatusUpdateRequest;
import com.insurance.claims.api.dto.ClaimUpdateRequest;
import com.insurance.claims.domain.ClaimDocument;
import com.insurance.claims.domain.ClaimStatus;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.service.ClaimService;
import com.insurance.claims.service.ai.ClaimAiIntakeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {
    private final ClaimService claimService;
    private final ClaimAiIntakeService claimAiIntakeService;

    public ClaimController(ClaimService claimService,
                           ClaimAiIntakeService claimAiIntakeService) {
        this.claimService = claimService;
        this.claimAiIntakeService = claimAiIntakeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponse create(@AuthenticationPrincipal SystemUser user,
                                @Valid @RequestBody ClaimRequest request) {
        return claimService.create(request, user);
    }

    @PostMapping("/ai-intake")
    @ResponseStatus(HttpStatus.CREATED)
    public AiClaimIntakeResponse createViaAi(@AuthenticationPrincipal SystemUser user,
                                             @Valid @RequestBody AiClaimIntakeRequest request) {
        return claimAiIntakeService.register(request, user);
    }

    @GetMapping
    public List<ClaimResponse> findAll(@AuthenticationPrincipal SystemUser user,
                                       @RequestParam(required = false) ClaimStatus status,
                                       @RequestParam(required = false) Long policyId) {
        return claimService.findAll(status, policyId, user);
    }

    @GetMapping("/{id}")
    public ClaimResponse findById(@AuthenticationPrincipal SystemUser user,
                                  @PathVariable Long id) {
        return claimService.findById(id, user);
    }

    @PutMapping("/{id}")
    public ClaimResponse updateDetails(@AuthenticationPrincipal SystemUser user,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ClaimUpdateRequest request) {
        return claimService.updateDetails(id, request, user);
    }

    @PatchMapping("/{id}/status")
    public ClaimResponse updateStatus(@AuthenticationPrincipal SystemUser user,
                                      @PathVariable Long id,
                                      @Valid @RequestBody ClaimStatusUpdateRequest request) {
        return claimService.updateStatus(id, request, user);
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimNoteResponse addNote(@AuthenticationPrincipal SystemUser user,
                                     @PathVariable Long id,
                                     @Valid @RequestBody ClaimNoteRequest request) {
        return claimService.addNote(id, request, user);
    }

    @GetMapping("/{id}/notes")
    public List<ClaimNoteResponse> findNotes(@AuthenticationPrincipal SystemUser user,
                                             @PathVariable Long id) {
        return claimService.findNotes(id, user);
    }

    @GetMapping("/{id}/history")
    public List<ClaimStatusHistoryResponse> findHistory(@AuthenticationPrincipal SystemUser user,
                                                        @PathVariable Long id) {
        return claimService.findHistory(id, user);
    }

    @GetMapping("/{id}/ai-assessment")
    public ClaimAiAssessmentResponse findAiAssessment(@AuthenticationPrincipal SystemUser user,
                                                      @PathVariable Long id) {
        return claimAiIntakeService.findLatestAssessment(id, user);
    }

    @GetMapping("/{id}/details")
    public List<ClaimDetailResponse> findDetails(@AuthenticationPrincipal SystemUser user,
                                                 @PathVariable Long id) {
        return claimService.findDetails(id, user);
    }

    @PostMapping("/{id}/details")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetailResponse addDetail(@AuthenticationPrincipal SystemUser user,
                                         @PathVariable Long id,
                                         @Valid @RequestBody ClaimDetailRequest request) {
        return claimService.addDetail(id, request, user);
    }

    @PutMapping("/{id}/details/{detailId}")
    public ClaimDetailResponse updateDetail(@AuthenticationPrincipal SystemUser user,
                                            @PathVariable Long id,
                                            @PathVariable Long detailId,
                                            @Valid @RequestBody ClaimDetailRequest request) {
        return claimService.updateDetail(id, detailId, request, user);
    }

    @DeleteMapping("/{id}/details/{detailId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDetail(@AuthenticationPrincipal SystemUser user,
                             @PathVariable Long id,
                             @PathVariable Long detailId) {
        claimService.deleteDetail(id, detailId, user);
    }

    @GetMapping("/{id}/documents/{documentId}")
    public ResponseEntity<byte[]> downloadDocument(@AuthenticationPrincipal SystemUser user,
                                                   @PathVariable Long id,
                                                   @PathVariable Long documentId) {
        ClaimDocument document = claimService.getDocument(id, documentId, user);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                .body(document.getData());
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@AuthenticationPrincipal SystemUser user,
                               @PathVariable Long id,
                               @PathVariable Long documentId) {
        claimService.deleteDocument(id, documentId, user);
    }
}
