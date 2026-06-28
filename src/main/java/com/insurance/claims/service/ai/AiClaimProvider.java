package com.insurance.claims.service.ai;

import com.insurance.claims.api.dto.AiClaimIntakeRequest;

public interface AiClaimProvider {
    AiClaimExtraction extract(AiClaimIntakeRequest request);

    String modelName();
}
