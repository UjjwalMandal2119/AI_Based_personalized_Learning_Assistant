package com.learningassistant.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiProviderRouterTest {

    @Mock OllamaAiProvider ollama;
    @Mock GeminiAiProvider gemini;

    AiProviderRouter router;

    @BeforeEach
    void setUp() {
        given(ollama.getProviderName()).willReturn("ollama");
        given(ollama.getModelName()).willReturn("llama3.2:3b");
        given(gemini.getProviderName()).willReturn("gemini");
        given(gemini.getModelName()).willReturn("gemini-3.8-flash");
        router = new AiProviderRouter(ollama, gemini, true);
    }

    @Test
    void ollamaSuccessReturnsResultWithoutFallback() {
        given(ollama.generateText("prompt")).willReturn("Ollama response");

        AiGenerationResult result = router.generate("prompt");

        assertThat(result.text()).isEqualTo("Ollama response");
        assertThat(result.providerName()).isEqualTo("ollama");
        assertThat(result.fallbackUsed()).isFalse();
        verify(gemini, never()).generateText("prompt");
    }

    @Test
    void ollamaUnavailableActivatesGeminiFallback() {
        given(ollama.generateText("prompt")).willThrow(
                new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE, "Ollama unreachable", true));
        given(gemini.isAvailable()).willReturn(true);
        given(gemini.generateText("prompt")).willReturn("Gemini response");

        AiGenerationResult result = router.generate("prompt");

        assertThat(result.text()).isEqualTo("Gemini response");
        assertThat(result.providerName()).isEqualTo("gemini");
        assertThat(result.fallbackUsed()).isTrue();
    }

    @Test
    void ollamaTimeoutActivatesGeminiFallback() {
        given(ollama.generateText("prompt")).willThrow(
                new AiProviderException(HttpStatus.BAD_GATEWAY, "Ollama timeout", true));
        given(gemini.isAvailable()).willReturn(true);
        given(gemini.generateText("prompt")).willReturn("Gemini fallback");

        AiGenerationResult result = router.generate("prompt");

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.providerName()).isEqualTo("gemini");
    }

    @Test
    void bothProvidersFailThrowsException() {
        given(ollama.generateText("prompt")).willThrow(
                new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE, "Ollama down", true));
        given(gemini.isAvailable()).willReturn(true);
        given(gemini.generateText("prompt")).willThrow(
                new AiProviderException(HttpStatus.BAD_GATEWAY, "Gemini error", true));

        assertThatThrownBy(() -> router.generate("prompt"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("Both AI providers failed");
    }

    @Test
    void geminiUnavailableWhenOllamaFailsThrowsException() {
        given(ollama.generateText("prompt")).willThrow(
                new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE, "Ollama down", true));
        given(gemini.isAvailable()).willReturn(false);

        assertThatThrownBy(() -> router.generate("prompt"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("Both AI providers are unavailable");
    }

    @Test
    void nonRetryableOllamaFailureDoesNotTriggerFallback() {
        given(ollama.generateText("prompt")).willThrow(
                new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE, "Ollama disabled", false));

        assertThatThrownBy(() -> router.generate("prompt"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("Ollama disabled");
        verify(gemini, never()).generateText("prompt");
    }

    @Test
    void fallbackDisabledDoesNotCallGemini() {
        router = new AiProviderRouter(ollama, gemini, false);
        given(ollama.generateText("prompt")).willThrow(
                new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE, "Ollama down", true));

        assertThatThrownBy(() -> router.generate("prompt"))
                .isInstanceOf(AiProviderException.class);
        verify(gemini, never()).generateText("prompt");
    }

    @Test
    void ollamaIsCalledBeforeGemini() {
        // Verifies call order: Ollama must be attempted first, Gemini only after Ollama fails
        given(ollama.generateText("prompt")).willThrow(
                new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE, "Ollama down", true));
        given(gemini.isAvailable()).willReturn(true);
        given(gemini.generateText("prompt")).willReturn("Gemini response");

        router.generate("prompt");

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(ollama, gemini);
        order.verify(ollama).generateText("prompt");
        order.verify(gemini).generateText("prompt");
    }

    @Test
    void successfulOllamaResultHasPositiveDuration() {
        given(ollama.generateText("prompt")).willReturn("response text");

        AiGenerationResult result = router.generate("prompt");

        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
        assertThat(result.text()).isEqualTo("response text");
        assertThat(result.modelName()).isEqualTo("llama3.2:3b");
    }

    @Test
    void malformedResponseFromOllamaIsRetryableAndTriggersFallback() {
        // BAD_GATEWAY with retryable=true should activate fallback
        given(ollama.generateText("prompt")).willThrow(
                new AiProviderException(HttpStatus.BAD_GATEWAY, "Ollama malformed response", true));
        given(gemini.isAvailable()).willReturn(true);
        given(gemini.generateText("prompt")).willReturn("Gemini fallback");

        AiGenerationResult result = router.generate("prompt");

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.providerName()).isEqualTo("gemini");
    }
}
