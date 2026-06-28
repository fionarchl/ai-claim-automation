package com.insurance.claims.api;

import com.insurance.claims.api.dto.AiConfigurationResponse;
import com.insurance.claims.api.dto.AiProviderSettingsRequest;
import com.insurance.claims.api.dto.AiProviderSettingsResponse;
import com.insurance.claims.api.dto.AiRuleSettingRequest;
import com.insurance.claims.api.dto.AiRuleSettingResponse;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.service.ai.AiConfigurationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/ai-configuration")
public class AiConfigurationController {
    private final AiConfigurationService aiConfigurationService;

    public AiConfigurationController(AiConfigurationService aiConfigurationService) {
        this.aiConfigurationService = aiConfigurationService;
    }

    @GetMapping
    public AiConfigurationResponse findConfiguration(@AuthenticationPrincipal SystemUser user) {
        return aiConfigurationService.findConfiguration(user);
    }

    @PutMapping("/provider")
    public AiProviderSettingsResponse updateProviderSettings(@AuthenticationPrincipal SystemUser user,
                                                             @Valid @RequestBody AiProviderSettingsRequest request) {
        return aiConfigurationService.updateProviderSettings(AiConfigurationService.PURPOSE_EXTRACTION, request, user);
    }

    @PutMapping("/extraction")
    public AiProviderSettingsResponse updateExtractionSettings(@AuthenticationPrincipal SystemUser user,
                                                               @Valid @RequestBody AiProviderSettingsRequest request) {
        return aiConfigurationService.updateProviderSettings(AiConfigurationService.PURPOSE_EXTRACTION, request, user);
    }

    @PutMapping("/reasoning")
    public AiProviderSettingsResponse updateReasoningSettings(@AuthenticationPrincipal SystemUser user,
                                                              @Valid @RequestBody AiProviderSettingsRequest request) {
        return aiConfigurationService.updateProviderSettings(AiConfigurationService.PURPOSE_REASONING, request, user);
    }

    @PutMapping("/rules")
    public List<AiRuleSettingResponse> updateRuleSettings(@AuthenticationPrincipal SystemUser user,
                                                          @Valid @RequestBody List<AiRuleSettingRequest> requests) {
        return aiConfigurationService.updateRuleSettings(requests, user);
    }
}
