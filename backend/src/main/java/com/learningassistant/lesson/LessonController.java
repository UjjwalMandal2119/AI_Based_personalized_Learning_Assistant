package com.learningassistant.lesson;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LessonController {
    private final LessonService lessons;

    public LessonController(LessonService lessons) {
        this.lessons = lessons;
    }

    @PostMapping("/learning-goals/{goalId}/lessons/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonDtos.LessonResponse generate(
            @AuthenticationPrincipal(expression = "username") String email,
            @PathVariable UUID goalId) {
        return lessons.generate(email, goalId);
    }

    @GetMapping("/learning-goals/{goalId}/lessons")
    public List<LessonDtos.LessonResponse> list(
            @AuthenticationPrincipal(expression = "username") String email,
            @PathVariable UUID goalId) {
        return lessons.list(email, goalId);
    }

    @GetMapping("/lessons/{lessonId}")
    public LessonDtos.LessonResponse get(
            @AuthenticationPrincipal(expression = "username") String email,
            @PathVariable UUID lessonId) {
        return lessons.get(email, lessonId);
    }
}
