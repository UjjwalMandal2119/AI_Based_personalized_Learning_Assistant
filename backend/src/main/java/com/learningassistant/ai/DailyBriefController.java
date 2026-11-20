package com.learningassistant.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/daily-brief")
public class DailyBriefController {

    private final DailyBriefService dailyBriefService;

    public DailyBriefController(DailyBriefService dailyBriefService) {
        this.dailyBriefService = dailyBriefService;
    }

    @GetMapping
    public ResponseEntity<DailyBriefResponse> getToday(
            @AuthenticationPrincipal(expression = "username") String email) {
        return dailyBriefService.getToday(email)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/generate")
    public ResponseEntity<DailyBriefResponse> generateToday(
            @AuthenticationPrincipal(expression = "username") String email) {
        return dailyBriefService.generateToday(email)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<DailyBriefResponse> refreshToday(
            @AuthenticationPrincipal(expression = "username") String email) {
        // Force regenerate by not checking cache
        return dailyBriefService.generateToday(email)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    private DailyBriefResponse toResponse(DailyBrief b) {
        return new DailyBriefResponse(
                b.getId(), b.getBriefDate(), b.getFocusSummary(), b.getPriorityTasks(),
                b.getRevisionTopics(), b.getDailyObjective(), b.getRecommendedActions(),
                b.getProviderName(), b.getModelName(), b.getGenerationDurationMs(),
                b.isFallbackUsed(), b.getCreatedAt()
        );
    }

    public record DailyBriefResponse(
            UUID id,
            java.time.LocalDate briefDate,
            String focusSummary,
            String priorityTasks,
            String revisionTopics,
            String dailyObjective,
            java.util.List<String> recommendedActions,
            String providerName,
            String modelName,
            Long generationDurationMs,
            boolean fallbackUsed,
            Instant createdAt
    ) {}
}