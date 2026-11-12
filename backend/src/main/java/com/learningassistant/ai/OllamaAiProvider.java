package com.learningassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class OllamaAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiProvider.class);

    private final RestClient restClient;
    private final String model;
    private final boolean enabled;

    public OllamaAiProvider(
            @Value("${ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ai.ollama.model:llama3.2:3b}") String model,
            @Value("${ai.ollama.enabled:true}") boolean enabled,
            @Value("${ai.ollama.connect-timeout:5s}") Duration connectTimeout,
            @Value("${ai.ollama.response-timeout:60s}") Duration responseTimeout) {
        this.model = model;
        this.enabled = enabled;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(responseTimeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public String getProviderName() { return "ollama"; }

    @Override
    public String getModelName() { return model; }

    @Override
    public boolean isAvailable() {
        if (!enabled) return false;
        try {
            restClient.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.debug("Ollama availability check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String generateText(String prompt) {
        if (!enabled) {
            throw new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Ollama provider is disabled", false);
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "options", Map.of("temperature", 0.3, "num_predict", 350));
        try {
            JsonNode response = restClient.post()
                    .uri("/api/generate")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return extractResponse(response);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 404) {
                throw new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Ollama model '" + model + "' not found", true, e);
            }
            throw new AiProviderException(HttpStatus.BAD_GATEWAY,
                    "Ollama returned HTTP " + status, true, e);
        } catch (ResourceAccessException e) {
            throw new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Ollama is unreachable", true, e);
        }
    }

    private String extractResponse(JsonNode node) {
        if (node == null || !node.hasNonNull("response")) {
            throw new AiProviderException(HttpStatus.BAD_GATEWAY,
                    "Ollama returned an empty or malformed response", true);
        }
        String text = node.get("response").asText("").trim();
        if (text.isBlank()) {
            throw new AiProviderException(HttpStatus.BAD_GATEWAY,
                    "Ollama returned an empty response", true);
        }
        return text;
    }
}
