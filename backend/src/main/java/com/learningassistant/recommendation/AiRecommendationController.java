package com.learningassistant.recommendation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/recommendations")
public class AiRecommendationController {

    private final AiRecommendationService service;

    public AiRecommendationController(AiRecommendationService service) {
        this.service = service;
    }

    /** Returns the latest cached recommendation, or 204 if none exists. */
    @GetMapping
    public ResponseEntity<AiRecommendationDtos.RecommendationResponse> getLatest(
            @AuthenticationPrincipal(expression = "username") String email) {
        return service.getLatest(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /** Generates a fresh recommendation (or returns cached if recent). */
    @PostMapping("/generate")
    public ResponseEntity<AiRecommendationDtos.RecommendationResponse> generate(
            @AuthenticationPrincipal(expression = "username") String email) {
        return service.getOrGenerate(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
