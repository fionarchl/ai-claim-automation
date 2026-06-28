package com.insurance.claims.service.ai;

import java.util.Arrays;
import java.util.List;

public final class AiRuleDefaults {
    public static final String FAIL = "FAIL";
    public static final String WARN = "WARN";
    public static final String SKIP = "SKIP";

    private AiRuleDefaults() {
    }

    public static List<Definition> definitions() {
        return Arrays.asList(
                new Definition("POLICY_ACTIVE", "Policy active", true, FAIL),
                new Definition("POLICY_BLACKLIST", "Policy blacklist", true, FAIL),
                new Definition("CLAIM_DATES_PRESENT", "Claim dates present", true, WARN),
                new Definition("CLAIM_DATE_ORDER", "Claim date order", true, FAIL),
                new Definition("POLICY_COVERAGE_DATES", "Coverage dates", true, FAIL),
                new Definition("CLAIM_AMOUNT_PRESENT", "Claim amount present", true, WARN),
                new Definition("COVERAGE_LIMIT", "Coverage limit", true, FAIL),
                new Definition("DOCUMENTS_PRESENT", "Required documents", true, FAIL),
                new Definition("BILLING_DOCUMENT", "Billing evidence", true, WARN),
                new Definition("DUPLICATE_CLAIM", "Duplicate claim", true, WARN),
                new Definition("AI_CONFIDENCE", "AI confidence", true, WARN)
        );
    }

    public static Definition definition(String code) {
        for (Definition definition : definitions()) {
            if (definition.getCode().equals(code)) {
                return definition;
            }
        }
        return null;
    }

    public static class Definition {
        private final String code;
        private final String label;
        private final boolean enabled;
        private final String failureOutcome;

        Definition(String code, String label, boolean enabled, String failureOutcome) {
            this.code = code;
            this.label = label;
            this.enabled = enabled;
            this.failureOutcome = failureOutcome;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getFailureOutcome() {
            return failureOutcome;
        }
    }
}
