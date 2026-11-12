package com.learningassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Fallback AI provider backed by the Google Gemini REST API.
 * Reuses the existing {@code geminiRestClient} bean.
 */
@Component
public class GeminiAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public GeminiAiProvider(
            RestClient geminiRestClient,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.8-flash}") String model,
            @Value("${ai.gemini.enabled:true}") boolean enabled) {
        this.restClient = geminiRestClient;
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
    }

    @Override
    public String getProviderName() { return "gemini"; }

    @Override
    public String getModelName() { return model; }

    @Override
    public boolean isAvailable() {
        return enabled && !apiKey.isBlank();
    }

    @Override
    public String generateText(String prompt) {
        if (!enabled || apiKey.isBlank()) {
            throw new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini provider is not configured", false);
        }
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("maxOutputTokens", 1024, "temperature", 0.5));
        try {
            JsonNode response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return extractText(response);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 429) {
                throw new AiProviderException(HttpStatus.TOO_MANY_REQUESTS,
                        "Gemini rate limit reached", true, e);
            }
            if (status == 401 || status == 403) {
                throw new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Gemini authentication failed", false, e);
            }
            throw new AiProviderException(HttpStatus.BAD_GATEWAY,
                    "Gemini returned HTTP " + status, true, e);
        } catch (ResourceAccessException e) {
            throw new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini is unreachable", true, e);
        }
    }

    private String extractText(JsonNode response) {
        JsonNode textNode = response == null ? null
                : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode == null || !textNode.isTextual() || textNode.asText().isBlank()) {
            throw new AiProviderException(HttpStatus.BAD_GATEWAY,
                    "Gemini returned an empty or malformed response", true);
        }
        return textNode.asText().trim();
    }
}
