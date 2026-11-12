package com.learningassistant.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/status")
public class AiStatusController {

    private final AiProviderRouter router;

    public AiStatusController(AiProviderRouter router) {
        this.router = router;
    }

    /** Returns AI provider availability. Requires authentication. */
    @GetMapping
    public ResponseEntity<AiStatusResponse> status(
            @AuthenticationPrincipal(expression = "username") String email) {
        return ResponseEntity.ok(new AiStatusResponse(
                router.getPrimaryProviderName(),
                router.isOllamaAvailable(),
                router.isGeminiAvailable(),
                router.isFallbackEnabled()));
    }

    public record AiStatusResponse(
            String primaryProvider,
            boolean ollamaAvailable,
            boolean geminiAvailable,
            boolean fallbackEnabled) {}
}
