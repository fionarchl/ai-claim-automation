package com.insurance.claims.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.claims.api.dto.RuleResultResponse;
import com.insurance.claims.domain.AiProviderSettings;
import com.insurance.claims.domain.Policy;
import com.insurance.claims.exception.BadRequestException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiClaimReasoningService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final AiConfigurationService aiConfigurationService;

    public AiClaimReasoningService(ObjectMapper objectMapper,
                                   AiConfigurationService aiConfigurationService) {
        this.objectMapper = objectMapper;
        this.aiConfigurationService = aiConfigurationService;
    }

    public AiReasoningResult reason(AiClaimExtraction extraction, Policy policy, AiRuleEvaluation evaluation) {
        AiProviderSettings settings = aiConfigurationService.getReasoningSettings();
        if ("test".equalsIgnoreCase(settings.getMode())) {
            return testReasoning(evaluation, settings);
        }
        if ("provider".equalsIgnoreCase(settings.getMode())) {
            return providerReasoning(extraction, policy, evaluation, settings);
        }
        throw new BadRequestException("Unsupported AI reasoning mode: " + settings.getMode());
    }

    private AiReasoningResult testReasoning(AiRuleEvaluation evaluation, AiProviderSettings settings) {
        AiReasoningResult result = new AiReasoningResult();
        result.setSummary(evaluation.getSummary());
        result.setModelName(settings.getProviderModel());
        for (RuleResultResponse rule : evaluation.getRuleResults()) {
            if (!"PASS".equals(rule.getOutcome())) {
                result.getEvidence().add(rule.getLabel() + ": " + rule.getMessage());
            }
        }
        return result;
    }

    private AiReasoningResult providerReasoning(AiClaimExtraction extraction,
                                                Policy policy,
                                                AiRuleEvaluation evaluation,
                                                AiProviderSettings settings) {
        String apiKey = aiConfigurationService.effectiveApiKey(settings);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return fallbackReasoning(evaluation, settings,
                    "AI reasoning provider is not configured; backend rule summary was used.");
        }
        try {
            JsonNode response = restTemplate.postForObject(
                    normalizeEndpoint(settings.getProviderEndpoint()),
                    new HttpEntity<Map<String, Object>>(body(extraction, policy, evaluation, settings), headers(apiKey)),
                    JsonNode.class);
            String content = response == null ? null : response.at("/choices/0/message/content").asText(null);
            if (content == null || content.trim().isEmpty()) {
                return fallbackReasoning(evaluation, settings,
                        "AI reasoning provider returned an empty response; backend rule summary was used.");
            }
            AiReasoningResult result = objectMapper.readValue(extractJson(content), AiReasoningResult.class);
            result.setModelName(settings.getProviderModel());
            return result;
        } catch (JsonProcessingException ex) {
            return fallbackReasoning(evaluation, settings,
                    "AI reasoning provider returned invalid JSON; backend rule summary was used.");
        } catch (BadRequestException ex) {
            return fallbackReasoning(evaluation, settings,
                    "AI reasoning provider returned a non-JSON response; backend rule summary was used.");
        } catch (RestClientException ex) {
            return fallbackReasoning(evaluation, settings,
                    "AI reasoning provider request failed; backend rule summary was used.");
        }
    }

    private AiReasoningResult fallbackReasoning(AiRuleEvaluation evaluation,
                                                AiProviderSettings settings,
                                                String reason) {
        AiReasoningResult result = testReasoning(evaluation, settings);
        result.setModelName(firstNonBlank(settings.getProviderModel(), "reasoning-provider") + " (fallback)");
        result.getEvidence().add(reason);
        return result;
    }

    private Map<String, Object> body(AiClaimExtraction extraction,
                                     Policy policy,
                                     AiRuleEvaluation evaluation,
                                     AiProviderSettings settings) throws JsonProcessingException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", settings.getProviderModel());
        body.put("temperature", settings.getTemperature());
        body.put("response_format", responseFormat());
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        messages.add(message("system", systemPrompt()));
        messages.add(message("user", userPrompt(extraction, policy, evaluation)));
        body.put("messages", messages);
        return body;
    }

    private String systemPrompt() {
        return "You are an insurance claim reasoning assistant. Return only a valid JSON object with no markdown. "
                + "Use this exact shape with concrete JSON values: "
                + "{\"summary\":\"Brief analyst-facing recommendation summary.\","
                + "\"evidence\":[\"Relevant rule or document evidence.\"]}. "
                + "Do not return schema placeholders such as string or string[]. "
                + "Do not make the final claim decision. Explain the backend rule result for a human analyst.";
    }

    private String userPrompt(AiClaimExtraction extraction, Policy policy, AiRuleEvaluation evaluation) throws JsonProcessingException {
        return "Write an analyst-facing recommendation summary from this structured context.\n"
                + "Recommended decision from deterministic rules: " + evaluation.getRecommendedDecision() + "\n"
                + "Policy number: " + policy.getPolicyNumber() + "\n"
                + "Policy type: " + policy.getType() + "\n"
                + "Coverage amount: " + policy.getCoverageAmount() + "\n"
                + "Extracted claim data JSON:\n" + objectMapper.writeValueAsString(extraction) + "\n"
                + "Rule results JSON:\n" + objectMapper.writeValueAsString(evaluation.getRuleResults());
    }

    private HttpHeaders headers(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private Map<String, String> responseFormat() {
        Map<String, String> format = new LinkedHashMap<String, String>();
        format.put("type", "json_object");
        return format;
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<String, Object>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String normalizeEndpoint(String configuredEndpoint) {
        String value = configuredEndpoint == null ? "" : configuredEndpoint.trim();
        if (value.endsWith("/chat/completions")) {
            return value;
        }
        if (value.endsWith("/")) {
            return value + "chat/completions";
        }
        return value + "/chat/completions";
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        if (trimmed.startsWith("<")) {
            throw new BadRequestException("AI reasoning provider returned markup instead of JSON. Check the reasoning endpoint and model.");
        }
        return trimmed;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        return second;
    }
}
