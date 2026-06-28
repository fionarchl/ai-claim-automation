package com.insurance.claims.api;

import com.insurance.claims.api.dto.PolicyBlacklistRequest;
import com.insurance.claims.api.dto.PolicyBlacklistResponse;
import com.insurance.claims.api.dto.PolicyResponse;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.service.PolicyService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {
    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public List<PolicyResponse> findAll() {
        return policyService.findAll();
    }

    @GetMapping("/{id}")
    public PolicyResponse findById(@PathVariable Long id) {
        return policyService.findById(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<PolicyResponse> findByCustomer(@PathVariable Long customerId) {
        return policyService.findByCustomer(customerId);
    }

    @GetMapping("/{id}/blacklist")
    public List<PolicyBlacklistResponse> findBlacklistEntries(@PathVariable Long id) {
        return policyService.findActiveBlacklistEntries(id);
    }

    @PostMapping("/{id}/blacklist")
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyBlacklistResponse addBlacklistEntry(@AuthenticationPrincipal SystemUser user,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody PolicyBlacklistRequest request) {
        return policyService.addBlacklistEntry(id, request, user);
    }

    @PatchMapping("/{id}/blacklist/{entryId}/resolve")
    public PolicyBlacklistResponse resolveBlacklistEntry(@AuthenticationPrincipal SystemUser user,
                                                         @PathVariable Long id,
                                                         @PathVariable Long entryId) {
        return policyService.resolveBlacklistEntry(id, entryId, user);
    }
}
