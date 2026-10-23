package com.learningassistant.task;

import com.learningassistant.common.ResourceNotFoundException;
import com.learningassistant.learninggoal.LearningGoal;
import com.learningassistant.learninggoal.LearningGoalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StudyTaskService {
    private final StudyTaskRepository tasks;
    private final LearningGoalService goals;

    public StudyTaskService(StudyTaskRepository tasks, LearningGoalService goals) {
        this.tasks = tasks;
        this.goals = goals;
    }

    @Transactional(readOnly = true)
    public List<TaskDtos.TaskResponse> list(String email, UUID goalId) {
        goals.ownedGoal(email, goalId);
        return tasks.findAllByLearningGoal_IdAndLearningGoal_Student_EmailOrderByDueDateAsc(goalId, email)
                .stream().map(TaskDtos.TaskResponse::from).toList();
    }

    @Transactional
    public TaskDtos.TaskResponse create(String email, UUID goalId, TaskDtos.TaskRequest request) {
        LearningGoal goal = goals.ownedGoal(email, goalId);
        StudyTask task = tasks.save(new StudyTask(goal, request.title().trim(), request.description(),
                request.taskType(), request.estimatedMinutes(), request.dueDate()));
        return TaskDtos.TaskResponse.from(task);
    }

    @Transactional
    public TaskDtos.TaskResponse update(String email, UUID taskId, TaskDtos.TaskRequest request) {
        StudyTask task = ownedTask(email, taskId);
        task.update(request.title().trim(), request.description(), request.taskType(),
                request.estimatedMinutes(), request.dueDate());
        return TaskDtos.TaskResponse.from(task);
    }

    @Transactional
    public TaskDtos.TaskResponse setCompletion(String email, UUID taskId, boolean completed) {
        StudyTask task = ownedTask(email, taskId);
        task.setCompleted(completed);
        return TaskDtos.TaskResponse.from(task);
    }

    StudyTask ownedTask(String email, UUID taskId) {
        return tasks.findByIdAndLearningGoal_Student_Email(taskId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Study task"));
    }
}
