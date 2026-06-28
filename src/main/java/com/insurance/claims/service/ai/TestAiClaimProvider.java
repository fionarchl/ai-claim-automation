package com.insurance.claims.service.ai;

import com.insurance.claims.api.dto.AiClaimIntakeRequest;
import com.insurance.claims.api.dto.ClaimDocumentRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TestAiClaimProvider implements AiClaimProvider {
    private static final Pattern POLICY_PATTERN = Pattern.compile("(POL-[A-Z]+-[0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?:Rp|IDR)?\\s*([0-9][0-9.,]{4,})", Pattern.CASE_INSENSITIVE);

    @Override
    public AiClaimExtraction extract(AiClaimIntakeRequest request) {
        String text = combinedText(request);
        AiClaimExtraction extraction = new AiClaimExtraction();
        extraction.setPolicyNumber(firstNonBlank(request.getPolicyNumber(), findPolicyNumber(text), "POL-HEALTH-0001"));
        extraction.setCustomerName(null);
        extraction.setAdmissionDate(LocalDate.now().minusDays(4));
        extraction.setDischargeDate(LocalDate.now().minusDays(1));
        extraction.setEstimatedAmount(findAmount(text));
        extraction.setDescription(firstNonBlank(request.getUserNote(), "AI intake claim generated from uploaded claim documents."));
        extraction.setConfidence(new BigDecimal("0.82"));
        extraction.getEvidence().add("Test AI mode extracted a claim draft from uploaded document metadata/text.");
        if (request.getDocuments() != null) {
            for (ClaimDocumentRequest document : request.getDocuments()) {
                extraction.getEvidence().add("Uploaded document: " + document.getFileName());
            }
        }
        return extraction;
    }

    @Override
    public String modelName() {
        return "test-ai-provider";
    }

    private String combinedText(AiClaimIntakeRequest request) {
        StringBuilder builder = new StringBuilder();
        if (request.getPolicyNumber() != null) {
            builder.append(request.getPolicyNumber()).append('\n');
        }
        if (request.getUserNote() != null) {
            builder.append(request.getUserNote()).append('\n');
        }
        if (request.getDocuments() == null) {
            return builder.toString();
        }
        for (ClaimDocumentRequest document : request.getDocuments()) {
            builder.append(document.getFileName()).append('\n');
            if (isTextLike(document.getContentType())) {
                try {
                    builder.append(new String(Base64.getDecoder().decode(document.getDataBase64()), StandardCharsets.UTF_8)).append('\n');
                } catch (IllegalArgumentException ignored) {
                    builder.append('\n');
                }
            }
        }
        return builder.toString();
    }

    private boolean isTextLike(String contentType) {
        return contentType != null && (contentType.startsWith("text/") || contentType.contains("json"));
    }

    private String findPolicyNumber(String text) {
        Matcher matcher = POLICY_PATTERN.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1).toUpperCase() : null;
    }

    private BigDecimal findAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return new BigDecimal("12500000");
        }
        String normalized = matcher.group(1).replace(".", "").replace(",", "");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return new BigDecimal("12500000");
        }
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return fallback;
    }

    private String firstNonBlank(String first, String fallback) {
        return firstNonBlank(first, null, fallback);
    }
}
