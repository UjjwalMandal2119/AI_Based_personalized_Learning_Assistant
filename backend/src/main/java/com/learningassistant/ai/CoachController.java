package com.learningassistant.ai;

import com.learningassistant.chat.ChatDtos;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/coach")
public class CoachController {

    private final CoachService coachService;

    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    @PostMapping("/respond")
    public ResponseEntity<CoachResponse> respond(
            @AuthenticationPrincipal(expression = "username") String email,
            @RequestBody CoachResponse.CoachRequest request) {
        CoachResponse response = coachService.respond(email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ChatDtos.ConversationSummary>> listConversations(
            @AuthenticationPrincipal(expression = "username") String email) {
        return ResponseEntity.ok(coachService.listConversations(email));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ChatDtos.ConversationDetail> getConversation(
            @AuthenticationPrincipal(expression = "username") String email,
            @PathVariable String conversationId) {
        return ResponseEntity.ok(coachService.getConversation(email, conversationId));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ChatDtos.ConversationDetail> createConversation(
            @AuthenticationPrincipal(expression = "username") String email,
            @RequestBody CoachResponse.CoachRequest request) {
        return ResponseEntity.ok(coachService.createConversation(email, request));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal(expression = "username") String email,
            @PathVariable String conversationId) {
        coachService.deleteConversation(email, conversationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/modes")
    public ResponseEntity<List<CoachMode>> getModes() {
        return ResponseEntity.ok(List.of(CoachMode.values()));
    }

    public enum CoachMode {
        LEARN("Learn", "Explain concepts step by step with examples"),
        PRACTICE("Practice", "Generate questions and evaluate your answers"),
        QUIZ("Quiz", "Interactive quizzes with scoring and feedback"),
        PLAN("Plan", "Create study schedules and learning plans"),
        INTERVIEW("Interview", "Mock interviews with detailed feedback"),
        IMPROVE("Improve", "Analyze performance and suggest improvements");

        private final String label;
        private final String description;

        CoachMode(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() { return label; }
        public String getDescription() { return description; }
    }
}