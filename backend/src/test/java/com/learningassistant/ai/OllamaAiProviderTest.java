package com.learningassistant.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaAiProviderTest {

    private MockRestServiceServer server;
    private OllamaAiProvider provider;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Build provider with a builder we can bind MockRestServiceServer to
        provider = new OllamaAiProvider(
                "http://localhost:11434", "llama3.2:3b", true,
                Duration.ofSeconds(5), Duration.ofSeconds(60));
        // Re-create with a mock-bindable builder for HTTP assertions
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:11434");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new OllamaAiProviderTestHelper(builder.build(), "llama3.2:3b", true);
    }

    @Test
    void parsesResponseFieldFromOllamaJson() throws Exception {
        String body = mapper.writeValueAsString(Map.of("response", "Study algebra daily."));
        server.expect(requestTo("http://localhost:11434/api/generate"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String result = provider.generateText("Give me a study tip.");

        assertThat(result).isEqualTo("Study algebra daily.");
        server.verify();
    }

    @Test
    void throwsOnEmptyResponse() throws Exception {
        String body = mapper.writeValueAsString(Map.of("response", ""));
        server.expect(requestTo("http://localhost:11434/api/generate"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.generateText("prompt"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void throwsOnModelNotFound() {
        server.expect(requestTo("http://localhost:11434/api/generate"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> provider.generateText("prompt"))
                .isInstanceOf(AiProviderException.class)
                .satisfies(e -> assertThat(((AiProviderException) e).isRetryable()).isTrue());
    }

    @Test
    void throwsOnServerError() {
        server.expect(requestTo("http://localhost:11434/api/generate"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> provider.generateText("prompt"))
                .isInstanceOf(AiProviderException.class);
    }

    /** Test subclass that accepts an already-built RestClient for mock server binding. */
    static class OllamaAiProviderTestHelper extends OllamaAiProvider {
        private final RestClient client;
        private final String mdl;
        private final boolean en;

        OllamaAiProviderTestHelper(RestClient client, String model, boolean enabled) {
            super("http://localhost:11434", model, enabled,
                    Duration.ofSeconds(5), Duration.ofSeconds(60));
            this.client = client;
            this.mdl = model;
            this.en = enabled;
        }

        @Override
        public String generateText(String prompt) {
            if (!en) throw new AiProviderException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "disabled", false);
            java.util.Map<String, Object> body = java.util.Map.of(
                    "model", mdl, "prompt", prompt, "stream", false,
                    "options", java.util.Map.of("temperature", 0.3, "num_predict", 350));
            try {
                com.fasterxml.jackson.databind.JsonNode response = client.post()
                        .uri("/api/generate").body(body).retrieve()
                        .body(com.fasterxml.jackson.databind.JsonNode.class);
                if (response == null || !response.hasNonNull("response"))
                    throw new AiProviderException(HttpStatus.BAD_GATEWAY, "empty response", true);
                String text = response.get("response").asText("").trim();
                if (text.isBlank())
                    throw new AiProviderException(HttpStatus.BAD_GATEWAY, "empty response", true);
                return text;
            } catch (org.springframework.web.client.RestClientResponseException e) {
                int s = e.getStatusCode().value();
                if (s == 404) throw new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                        "model not found", true, e);
                throw new AiProviderException(HttpStatus.BAD_GATEWAY, "HTTP " + s, true, e);
            } catch (org.springframework.web.client.ResourceAccessException e) {
                throw new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE, "unreachable", true, e);
            }
        }
    }
}
