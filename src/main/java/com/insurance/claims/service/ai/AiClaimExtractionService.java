package com.insurance.claims.service.ai;

import com.insurance.claims.api.dto.AiClaimIntakeRequest;
import com.insurance.claims.domain.AiProviderSettings;
import com.insurance.claims.exception.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class AiClaimExtractionService {
    private final AiConfigurationService aiConfigurationService;
    private final TestAiClaimProvider testAiClaimProvider;
    private final ProviderAiClaimProvider providerAiClaimProvider;

    public AiClaimExtractionService(AiConfigurationService aiConfigurationService,
                                    TestAiClaimProvider testAiClaimProvider,
                                    ProviderAiClaimProvider providerAiClaimProvider) {
        this.aiConfigurationService = aiConfigurationService;
        this.testAiClaimProvider = testAiClaimProvider;
        this.providerAiClaimProvider = providerAiClaimProvider;
    }

    public AiClaimExtraction extract(AiClaimIntakeRequest request) {
        AiProviderSettings settings = aiConfigurationService.getExtractionSettings();
        if ("test".equalsIgnoreCase(settings.getMode())) {
            return testAiClaimProvider.extract(request);
        }
        if ("provider".equalsIgnoreCase(settings.getMode())) {
            return providerAiClaimProvider.extract(request);
        }
        throw new BadRequestException("Unsupported AI mode: " + settings.getMode());
    }

    public String modelName() {
        AiProviderSettings settings = aiConfigurationService.getExtractionSettings();
        if ("provider".equalsIgnoreCase(settings.getMode())) {
            return providerAiClaimProvider.modelName();
        }
        return testAiClaimProvider.modelName();
    }
}
