package com.learningassistant.learninggoal;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning-goals")
public class LearningGoalController {
    private final LearningGoalService goals;

    public LearningGoalController(LearningGoalService goals) {
        this.goals = goals;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LearningGoalDtos.GoalResponse create(
            @org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "username") String email,
            @Valid @RequestBody LearningGoalDtos.GoalRequest request) {
        return goals.create(email, request);
    }

    @GetMapping
    public List<LearningGoalDtos.GoalResponse> list(
            @org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "username") String email) {
        return goals.list(email);
    }

    @GetMapping("/{goalId}")
    public LearningGoalDtos.GoalResponse get(
            @org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "username") String email,
            @PathVariable UUID goalId) {
        return goals.get(email, goalId);
    }

    @PutMapping("/{goalId}")
    public LearningGoalDtos.GoalResponse update(
            @org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "username") String email,
            @PathVariable UUID goalId, @Valid @RequestBody LearningGoalDtos.GoalRequest request) {
        return goals.update(email, goalId, request);
    }

    @DeleteMapping("/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "username") String email,
            @PathVariable UUID goalId) {
        goals.delete(email, goalId);
    }
}
