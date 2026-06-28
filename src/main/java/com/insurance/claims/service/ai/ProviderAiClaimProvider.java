package com.insurance.claims.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.claims.api.dto.AiClaimIntakeRequest;
import com.insurance.claims.api.dto.ClaimDocumentRequest;
import com.insurance.claims.domain.AiProviderSettings;
import com.insurance.claims.exception.BadRequestException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProviderAiClaimProvider implements AiClaimProvider {
    private static final int MAX_RENDERED_PDF_PAGES = 3;
    private static final int PDF_TEXT_RENDER_FALLBACK_THRESHOLD = 120;
    private static final float PDF_RENDER_DPI = 120F;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final AiConfigurationService aiConfigurationService;

    public ProviderAiClaimProvider(ObjectMapper objectMapper,
                                   AiConfigurationService aiConfigurationService) {
        this.objectMapper = objectMapper;
        this.aiConfigurationService = aiConfigurationService;
    }

    @Override
    public AiClaimExtraction extract(AiClaimIntakeRequest request) {
        AiProviderSettings settings = aiConfigurationService.getExtractionSettings();
        String apiKey = aiConfigurationService.effectiveApiKey(settings);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new BadRequestException("AI extraction provider mode requires CLAIMOPS_AI_PROVIDER_API_KEY, CLAIMOPS_AI_EXTRACTION_API_KEY, OPENAI_API_KEY, or GEMINI_API_KEY in .env");
        }
        try {
            JsonNode response = restTemplate.postForObject(
                    normalizeEndpoint(settings.getProviderEndpoint()),
                    new HttpEntity<Map<String, Object>>(body(request, settings), headers(apiKey)),
                    JsonNode.class);
            String content = response == null ? null : response.at("/choices/0/message/content").asText(null);
            if (content == null || content.trim().isEmpty()) {
                throw new BadRequestException("AI provider did not return an extraction payload");
            }
            return objectMapper.readValue(extractJson(content), AiClaimExtraction.class);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("AI provider returned JSON that could not be parsed: " + ex.getOriginalMessage());
        } catch (RestClientException ex) {
            throw new BadRequestException("AI provider request failed: " + ex.getMessage());
        }
    }

    @Override
    public String modelName() {
        return aiConfigurationService.getExtractionSettings().getProviderModel();
    }

    private HttpHeaders headers(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
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

    private Map<String, Object> body(AiClaimIntakeRequest request, AiProviderSettings settings) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", settings.getProviderModel());
        body.put("temperature", settings.getTemperature());
        body.put("response_format", responseFormat());
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        messages.add(message("system", systemPrompt()));
        messages.add(userMessage(request, settings.getTextPreviewLimit()));
        body.put("messages", messages);
        return body;
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

    private Map<String, Object> userMessage(AiClaimIntakeRequest request, int textPreviewLimit) {
        Map<String, Object> message = new LinkedHashMap<String, Object>();
        message.put("role", "user");

        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        Map<String, Object> textPart = new LinkedHashMap<String, Object>();
        textPart.put("type", "text");
        textPart.put("text", userPrompt(request, textPreviewLimit));
        content.add(textPart);

        if (request.getDocuments() != null) {
            for (ClaimDocumentRequest document : request.getDocuments()) {
                if (isImage(document)) {
                    addImagePart(content, "data:" + safeContentType(document) + ";base64," + document.getDataBase64());
                } else if (isPdf(document) && shouldRenderPdfPages(document, textPreviewLimit)) {
                    for (String dataUrl : renderedPdfPageDataUrls(document)) {
                        addImagePart(content, dataUrl);
                    }
                }
            }
        }

        message.put("content", content);
        return message;
    }

    private void addImagePart(List<Map<String, Object>> content, String dataUrl) {
        Map<String, Object> imagePart = new LinkedHashMap<String, Object>();
        imagePart.put("type", "image_url");
        Map<String, Object> imageUrl = new LinkedHashMap<String, Object>();
        imageUrl.put("url", dataUrl);
        imageUrl.put("detail", "auto");
        imagePart.put("image_url", imageUrl);
        content.add(imagePart);
    }

    private String systemPrompt() {
        return "You extract insurance claim intake data. Return only a valid JSON object with no markdown. "
                + "Use this exact shape with concrete JSON values: "
                + "{\"policyNumber\":\"POL-HEALTH-0001\","
                + "\"customerName\":\"Customer name or null\","
                + "\"admissionDate\":\"YYYY-MM-DD\","
                + "\"dischargeDate\":\"YYYY-MM-DD\","
                + "\"estimatedAmount\":12000000,"
                + "\"description\":\"Short claim description\","
                + "\"confidence\":0.86,"
                + "\"evidence\":[\"Relevant extracted evidence\"],"
                + "\"riskIndicators\":[],"
                + "\"claimDetails\":[{\"category\":\"Hospitalization\","
                + "\"eventStartDate\":\"YYYY-MM-DD\","
                + "\"eventEndDate\":\"YYYY-MM-DD\","
                + "\"submittedAmount\":12000000,"
                + "\"description\":\"Line item description\"}]}. "
                + "Use null only when a field is truly unavailable. Do not return schema placeholders such as string or number.";
    }

    private String userPrompt(AiClaimIntakeRequest request, int textPreviewLimit) {
        StringBuilder builder = new StringBuilder();
        builder.append("Create a claim draft from these uploaded claim documents.\n");
        if (request.getPolicyId() != null) {
            builder.append("Selected policy id: ").append(request.getPolicyId()).append('\n');
        }
        if (request.getPolicyNumber() != null && !request.getPolicyNumber().trim().isEmpty()) {
            builder.append("Selected policy number: ").append(request.getPolicyNumber()).append('\n');
        }
        if (request.getUserNote() != null && !request.getUserNote().trim().isEmpty()) {
            builder.append("User note: ").append(request.getUserNote()).append('\n');
        }
        builder.append("Documents:\n");
        if (request.getDocuments() != null) {
            for (ClaimDocumentRequest document : request.getDocuments()) {
                builder.append("- ").append(document.getFileName())
                        .append(" (").append(document.getContentType()).append(")\n");
                String text = textPreview(document, textPreviewLimit);
                if (!text.isEmpty()) {
                    builder.append("Extracted document text:\n");
                    builder.append(text).append('\n');
                } else if (isImage(document)) {
                    builder.append("Image document is attached as an image_url content part. Read visible text and claim evidence from the image.\n");
                } else if (isPdf(document)) {
                    builder.append("PDF pages may also be attached as rendered image_url content parts if extracted text is incomplete. Read visible text and claim evidence from the rendered PDF pages when present.\n");
                } else {
                    builder.append("Binary document content is stored with the claim but is not readable in this provider request. Extract from filename and available context if text is unavailable.\n");
                }
            }
        }
        return builder.toString();
    }

    private boolean isImage(ClaimDocumentRequest document) {
        return document.getContentType() != null && document.getContentType().toLowerCase().startsWith("image/");
    }

    private boolean isPdf(ClaimDocumentRequest document) {
        String contentType = document.getContentType() == null ? "" : document.getContentType().toLowerCase();
        String fileName = document.getFileName() == null ? "" : document.getFileName().toLowerCase();
        return contentType.contains("pdf") || fileName.endsWith(".pdf");
    }

    private String safeContentType(ClaimDocumentRequest document) {
        return document.getContentType() == null || document.getContentType().trim().isEmpty()
                ? "application/octet-stream"
                : document.getContentType().trim();
    }

    private String textPreview(ClaimDocumentRequest document, int textPreviewLimit) {
        if (isPdf(document)) {
            return pdfTextPreview(document, textPreviewLimit);
        }
        if (document.getContentType() == null
                || !(document.getContentType().startsWith("text/") || document.getContentType().contains("json"))) {
            return "";
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(document.getDataBase64()), StandardCharsets.UTF_8);
            if (decoded.length() > textPreviewLimit) {
                return decoded.substring(0, textPreviewLimit);
            }
            return decoded;
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private String pdfTextPreview(ClaimDocumentRequest document, int textPreviewLimit) {
        try {
            byte[] bytes = Base64.getDecoder().decode(document.getDataBase64());
            PDDocument pdf = PDDocument.load(bytes);
            try {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = normalizeText(stripper.getText(pdf));
                if (text.length() > textPreviewLimit) {
                    return text.substring(0, textPreviewLimit);
                }
                return text;
            } finally {
                pdf.close();
            }
        } catch (IOException ex) {
            return "";
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private boolean shouldRenderPdfPages(ClaimDocumentRequest document, int textPreviewLimit) {
        String text = textPreview(document, textPreviewLimit);
        return text.length() < PDF_TEXT_RENDER_FALLBACK_THRESHOLD;
    }

    private List<String> renderedPdfPageDataUrls(ClaimDocumentRequest document) {
        List<String> dataUrls = new ArrayList<String>();
        try {
            byte[] bytes = Base64.getDecoder().decode(document.getDataBase64());
            PDDocument pdf = PDDocument.load(bytes);
            try {
                PDFRenderer renderer = new PDFRenderer(pdf);
                int pageCount = Math.min(pdf.getNumberOfPages(), MAX_RENDERED_PDF_PAGES);
                for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                    BufferedImage image = renderer.renderImageWithDPI(pageIndex, PDF_RENDER_DPI, ImageType.RGB);
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    ImageIO.write(image, "png", output);
                    dataUrls.add("data:image/png;base64,"
                            + Base64.getEncoder().encodeToString(output.toByteArray()));
                }
            } finally {
                pdf.close();
            }
        } catch (IOException ex) {
            return dataUrls;
        } catch (IllegalArgumentException ex) {
            return dataUrls;
        }
        return dataUrls;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\r', '\n')
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        if (trimmed.startsWith("```")) {
            throw new BadRequestException("AI provider returned a fenced response without a JSON object.");
        }
        if (trimmed.startsWith("<")) {
            throw new BadRequestException("AI provider returned markup instead of JSON. Check that the selected model supports image/text chat completions and JSON responses.");
        }
        return trimmed;
    }

}
