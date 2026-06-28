package com.insurance.claims.service;

import com.insurance.claims.api.dto.PolicyRequest;
import com.insurance.claims.api.dto.PolicyBlacklistRequest;
import com.insurance.claims.api.dto.PolicyBlacklistResponse;
import com.insurance.claims.api.dto.PolicyResponse;
import com.insurance.claims.domain.Customer;
import com.insurance.claims.domain.Policy;
import com.insurance.claims.domain.PolicyBlacklistEntry;
import com.insurance.claims.exception.BadRequestException;
import com.insurance.claims.exception.ForbiddenException;
import com.insurance.claims.exception.ResourceNotFoundException;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.repository.PolicyBlacklistEntryRepository;
import com.insurance.claims.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PolicyService {
    private final PolicyRepository policyRepository;
    private final PolicyBlacklistEntryRepository policyBlacklistEntryRepository;
    private final CustomerService customerService;

    public PolicyService(PolicyRepository policyRepository,
                         PolicyBlacklistEntryRepository policyBlacklistEntryRepository,
                         CustomerService customerService) {
        this.policyRepository = policyRepository;
        this.policyBlacklistEntryRepository = policyBlacklistEntryRepository;
        this.customerService = customerService;
    }

    @Transactional
    public PolicyResponse create(PolicyRequest request) {
        if (policyRepository.existsByPolicyNumber(request.getPolicyNumber())) {
            throw new BadRequestException("Policy number already exists: " + request.getPolicyNumber());
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Policy end date must be on or after start date");
        }

        Customer customer = customerService.getCustomer(request.getCustomerId());
        Policy policy = new Policy();
        policy.setCustomer(customer);
        policy.setPolicyNumber(request.getPolicyNumber());
        policy.setType(request.getType());
        policy.setCoverageAmount(request.getCoverageAmount());
        policy.setStartDate(request.getStartDate());
        policy.setEndDate(request.getEndDate());
        policy.setStatus(request.getStatus());
        return toResponse(policyRepository.save(policy));
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> findAll() {
        return policyRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PolicyResponse findById(Long id) {
        return toResponse(getPolicy(id));
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> findByCustomer(Long customerId) {
        customerService.getCustomer(customerId);
        return policyRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Policy getPolicy(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PolicyBlacklistResponse> findActiveBlacklistEntries(Long policyId) {
        getPolicy(policyId);
        return policyBlacklistEntryRepository.findByPolicyIdAndActiveTrue(policyId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PolicyBlacklistResponse addBlacklistEntry(Long policyId, PolicyBlacklistRequest request, SystemUser user) {
        requireSystemAdministrator(user);
        Policy policy = getPolicy(policyId);
        PolicyBlacklistEntry entry = new PolicyBlacklistEntry();
        entry.markCreatedBy(actorName(user));
        entry.setPolicy(policy);
        entry.setReason(request.getReason());
        entry.setSeverity(request.getSeverity());
        entry.setActive(true);
        return toResponse(policyBlacklistEntryRepository.save(entry));
    }

    @Transactional
    public PolicyBlacklistResponse resolveBlacklistEntry(Long policyId, Long entryId, SystemUser user) {
        requireSystemAdministrator(user);
        PolicyBlacklistEntry entry = policyBlacklistEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy blacklist entry not found: " + entryId));
        if (!entry.getPolicy().getId().equals(policyId)) {
            throw new BadRequestException("Blacklist entry does not belong to policy: " + policyId);
        }
        entry.setActive(false);
        entry.setResolvedBy(actorName(user));
        entry.setResolvedAt(LocalDateTime.now());
        entry.markUpdatedBy(actorName(user));
        return toResponse(policyBlacklistEntryRepository.save(entry));
    }

    public PolicyResponse toResponse(Policy policy) {
        PolicyResponse response = new PolicyResponse();
        response.setId(policy.getId());
        response.setPolicyNumber(policy.getPolicyNumber());
        response.setCustomerId(policy.getCustomer().getId());
        response.setCustomerName(policy.getCustomer().getFullName());
        response.setType(policy.getType());
        response.setCoverageAmount(policy.getCoverageAmount());
        response.setStartDate(policy.getStartDate());
        response.setEndDate(policy.getEndDate());
        response.setStatus(policy.getStatus());
        List<PolicyBlacklistEntry> activeEntries = policyBlacklistEntryRepository.findByPolicyIdAndActiveTrue(policy.getId());
        response.setBlacklisted(!activeEntries.isEmpty());
        response.setBlacklistReason(activeEntries.isEmpty() ? null : activeEntries.get(0).getReason());
        return response;
    }

    private PolicyBlacklistResponse toResponse(PolicyBlacklistEntry entry) {
        PolicyBlacklistResponse response = new PolicyBlacklistResponse();
        response.setId(entry.getId());
        response.setPolicyId(entry.getPolicy().getId());
        response.setReason(entry.getReason());
        response.setSeverity(entry.getSeverity());
        response.setActive(entry.isActive());
        response.setResolvedBy(entry.getResolvedBy());
        response.setResolvedAt(entry.getResolvedAt());
        response.setCreatedAt(entry.getCreatedAt());
        return response;
    }

    private void requireSystemAdministrator(SystemUser user) {
        if (!RolePolicy.canManageUsers(user)) {
            throw new ForbiddenException("Only system administrators can manage policy blacklist entries");
        }
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
