package com.learningassistant.task;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class StudyTaskController {
    private final StudyTaskService tasks;

    public StudyTaskController(StudyTaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping("/learning-goals/{goalId}/tasks")
    public List<TaskDtos.TaskResponse> list(@AuthenticationPrincipal(expression = "username") String email,
                                            @PathVariable UUID goalId) {
        return tasks.list(email, goalId);
    }

    @PostMapping("/learning-goals/{goalId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDtos.TaskResponse create(@AuthenticationPrincipal(expression = "username") String email,
                                        @PathVariable UUID goalId,
                                        @Valid @RequestBody TaskDtos.TaskRequest request) {
        return tasks.create(email, goalId, request);
    }

    @PatchMapping("/tasks/{taskId}")
    public TaskDtos.TaskResponse update(@AuthenticationPrincipal(expression = "username") String email,
                                        @PathVariable UUID taskId,
                                        @Valid @RequestBody TaskDtos.TaskRequest request) {
        return tasks.update(email, taskId, request);
    }

    @PatchMapping("/tasks/{taskId}/completion")
    public TaskDtos.TaskResponse completion(@AuthenticationPrincipal(expression = "username") String email,
                                            @PathVariable UUID taskId,
                                            @Valid @RequestBody TaskDtos.CompletionRequest request) {
        return tasks.setCompletion(email, taskId, request.completed());
    }
}
