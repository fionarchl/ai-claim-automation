package com.insurance.claims.service.ai;

import com.insurance.claims.api.dto.AiConfigurationResponse;
import com.insurance.claims.api.dto.AiProviderSettingsRequest;
import com.insurance.claims.api.dto.AiProviderSettingsResponse;
import com.insurance.claims.api.dto.AiRuleSettingRequest;
import com.insurance.claims.api.dto.AiRuleSettingResponse;
import com.insurance.claims.domain.AiProviderSettings;
import com.insurance.claims.domain.AiRuleSetting;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.exception.BadRequestException;
import com.insurance.claims.exception.ForbiddenException;
import com.insurance.claims.repository.AiProviderSettingsRepository;
import com.insurance.claims.repository.AiRuleSettingRepository;
import com.insurance.claims.service.RolePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiConfigurationService {
    public static final String PURPOSE_EXTRACTION = "extraction";
    public static final String PURPOSE_REASONING = "reasoning";

    private final AiProviderSettingsRepository aiProviderSettingsRepository;
    private final AiRuleSettingRepository aiRuleSettingRepository;
    private final String defaultMode;
    private final String defaultEndpoint;
    private final String defaultApiKey;
    private final String defaultModel;

    public AiConfigurationService(AiProviderSettingsRepository aiProviderSettingsRepository,
                                  AiRuleSettingRepository aiRuleSettingRepository,
                                  @Value("${claimops.ai.mode:test}") String defaultMode,
                                  @Value("${claimops.ai.provider.endpoint:https://api.openai.com/v1/chat/completions}") String defaultEndpoint,
                                  @Value("${claimops.ai.provider.api-key:}") String defaultApiKey,
                                  @Value("${claimops.ai.provider.model:gpt-4o-mini}") String defaultModel) {
        this.aiProviderSettingsRepository = aiProviderSettingsRepository;
        this.aiRuleSettingRepository = aiRuleSettingRepository;
        this.defaultMode = defaultMode;
        this.defaultEndpoint = defaultEndpoint;
        this.defaultApiKey = defaultApiKey;
        this.defaultModel = defaultModel;
    }

    @Transactional
    public AiConfigurationResponse findConfiguration(SystemUser user) {
        requireSystemAdministrator(user);
        AiConfigurationResponse response = new AiConfigurationResponse();
        response.setExtractionSettings(toResponse(getExtractionSettings()));
        response.setReasoningSettings(toResponse(getReasoningSettings()));
        response.setRuleSettings(findRuleSettings().stream().map(this::toResponse).collect(Collectors.toList()));
        return response;
    }

    @Transactional
    public AiProviderSettingsResponse updateProviderSettings(AiProviderSettingsRequest request, SystemUser user) {
        return updateProviderSettings(PURPOSE_EXTRACTION, request, user);
    }

    @Transactional
    public AiProviderSettingsResponse updateProviderSettings(String purpose, AiProviderSettingsRequest request, SystemUser user) {
        requireSystemAdministrator(user);
        validateMode(request.getMode());
        validatePurpose(purpose);
        AiProviderSettings settings = getProviderSettings(purpose);
        settings.setMode(request.getMode().trim().toLowerCase());
        settings.setProviderEndpoint(request.getProviderEndpoint().trim());
        settings.setProviderModel(request.getProviderModel().trim());
        settings.setTemperature(request.getTemperature() == null ? BigDecimal.ZERO : request.getTemperature());
        settings.setTextPreviewLimit(request.getTextPreviewLimit() == null ? 8000 : request.getTextPreviewLimit());
        settings.setConfidenceThreshold(request.getConfidenceThreshold() == null
                ? new BigDecimal("0.65")
                : request.getConfidenceThreshold());
        settings.markUpdatedBy(actorName(user));
        return toResponse(aiProviderSettingsRepository.save(settings));
    }

    @Transactional
    public List<AiRuleSettingResponse> updateRuleSettings(List<AiRuleSettingRequest> requests, SystemUser user) {
        requireSystemAdministrator(user);
        for (AiRuleSettingRequest request : requests) {
            AiRuleDefaults.Definition definition = AiRuleDefaults.definition(request.getCode());
            if (definition == null) {
                throw new BadRequestException("Unsupported AI rule code: " + request.getCode());
            }
            validateOutcome(request.getFailureOutcome());
            AiRuleSetting setting = aiRuleSettingRepository.findByCode(request.getCode())
                    .orElseGet(() -> createRuleSetting(definition));
            setting.setEnabled(request.isEnabled());
            setting.setFailureOutcome(request.getFailureOutcome());
            setting.markUpdatedBy(actorName(user));
            aiRuleSettingRepository.save(setting);
        }
        return findRuleSettings().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AiProviderSettings getProviderSettings() {
        return getExtractionSettings();
    }

    @Transactional
    public AiProviderSettings getExtractionSettings() {
        return getProviderSettings(PURPOSE_EXTRACTION);
    }

    @Transactional
    public AiProviderSettings getReasoningSettings() {
        return getProviderSettings(PURPOSE_REASONING);
    }

    @Transactional
    public AiProviderSettings getProviderSettings(String purpose) {
        validatePurpose(purpose);
        return aiProviderSettingsRepository.findByPurpose(purpose)
                .orElseGet(() -> defaultProviderSettings(purpose));
    }

    @Transactional
    public List<AiRuleSetting> findRuleSettings() {
        List<AiRuleSetting> settings = aiRuleSettingRepository.findAll();
        if (settings.size() < AiRuleDefaults.definitions().size()) {
            return AiRuleDefaults.definitions().stream()
                    .map(definition -> aiRuleSettingRepository.findByCode(definition.getCode())
                            .orElseGet(() -> createRuleSetting(definition)))
                    .sorted(Comparator.comparing(AiRuleSetting::getCode))
                    .collect(Collectors.toList());
        }
        return settings.stream()
                .sorted(Comparator.comparing(AiRuleSetting::getCode))
                .collect(Collectors.toList());
    }

    public AiRuleSetting rule(String code) {
        AiRuleDefaults.Definition definition = AiRuleDefaults.definition(code);
        if (definition == null) {
            throw new BadRequestException("Unsupported AI rule code: " + code);
        }
        return aiRuleSettingRepository.findByCode(code)
                .orElseGet(() -> createRuleSetting(definition));
    }

    public String effectiveApiKey(AiProviderSettings settings) {
        return firstNonBlank(defaultApiKey,
                System.getenv("CLAIMOPS_AI_PROVIDER_API_KEY"),
                PURPOSE_EXTRACTION.equals(settings.getPurpose()) ? System.getenv("CLAIMOPS_AI_EXTRACTION_API_KEY") : System.getenv("CLAIMOPS_AI_REASONING_API_KEY"),
                System.getenv("OPENAI_API_KEY"),
                System.getenv("GEMINI_API_KEY"));
    }

    private AiProviderSettings defaultProviderSettings(String purpose) {
        if (PURPOSE_EXTRACTION.equals(purpose)) {
            AiProviderSettings legacy = aiProviderSettingsRepository.findTopByOrderByIdAsc().orElse(null);
            if (legacy != null && (legacy.getPurpose() == null || legacy.getPurpose().trim().isEmpty())) {
                legacy.setPurpose(PURPOSE_EXTRACTION);
                return aiProviderSettingsRepository.save(legacy);
            }
        }
        AiProviderSettings settings = new AiProviderSettings();
        settings.markCreatedBy("system");
        settings.setPurpose(purpose);
        settings.setMode(firstNonBlank(defaultMode, "test").toLowerCase());
        settings.setProviderEndpoint(firstNonBlank(defaultEndpoint, "https://api.openai.com/v1/chat/completions"));
        settings.setProviderModel(firstNonBlank(defaultModel, "gpt-4o-mini"));
        settings.setTemperature(BigDecimal.ZERO);
        settings.setTextPreviewLimit(8000);
        settings.setConfidenceThreshold(new BigDecimal("0.65"));
        return aiProviderSettingsRepository.save(settings);
    }

    private AiRuleSetting createRuleSetting(AiRuleDefaults.Definition definition) {
        AiRuleSetting setting = new AiRuleSetting();
        setting.markCreatedBy("system");
        setting.setCode(definition.getCode());
        setting.setLabel(definition.getLabel());
        setting.setEnabled(definition.isEnabled());
        setting.setFailureOutcome(definition.getFailureOutcome());
        return aiRuleSettingRepository.save(setting);
    }

    private AiProviderSettingsResponse toResponse(AiProviderSettings settings) {
        AiProviderSettingsResponse response = new AiProviderSettingsResponse();
        response.setPurpose(settings.getPurpose());
        response.setMode(settings.getMode());
        response.setProviderEndpoint(settings.getProviderEndpoint());
        response.setProviderModel(settings.getProviderModel());
        response.setProviderApiKeyConfigured(!effectiveApiKey(settings).isEmpty());
        response.setTemperature(settings.getTemperature());
        response.setTextPreviewLimit(settings.getTextPreviewLimit());
        response.setConfidenceThreshold(settings.getConfidenceThreshold());
        response.setUpdatedAt(settings.getUpdatedAt());
        return response;
    }

    private AiRuleSettingResponse toResponse(AiRuleSetting setting) {
        AiRuleSettingResponse response = new AiRuleSettingResponse();
        response.setCode(setting.getCode());
        response.setLabel(setting.getLabel());
        response.setEnabled(setting.isEnabled());
        response.setFailureOutcome(setting.getFailureOutcome());
        return response;
    }

    private void validateMode(String mode) {
        if (!"test".equalsIgnoreCase(mode) && !"provider".equalsIgnoreCase(mode)) {
            throw new BadRequestException("AI mode must be test or provider");
        }
    }

    private void validatePurpose(String purpose) {
        if (!PURPOSE_EXTRACTION.equals(purpose) && !PURPOSE_REASONING.equals(purpose)) {
            throw new BadRequestException("AI provider purpose must be extraction or reasoning");
        }
    }

    private void validateOutcome(String outcome) {
        if (!AiRuleDefaults.FAIL.equals(outcome)
                && !AiRuleDefaults.WARN.equals(outcome)
                && !AiRuleDefaults.SKIP.equals(outcome)) {
            throw new BadRequestException("Rule outcome must be FAIL, WARN, or SKIP");
        }
    }

    private void requireSystemAdministrator(SystemUser user) {
        if (!RolePolicy.canManageUsers(user)) {
            throw new ForbiddenException("Only system administrators can manage AI configuration");
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

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
