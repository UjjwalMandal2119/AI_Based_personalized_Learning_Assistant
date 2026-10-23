package com.learningassistant.learningplan;

import com.learningassistant.common.ResourceNotFoundException;
import com.learningassistant.learninggoal.GoalStatus;
import com.learningassistant.learninggoal.LearningGoalDtos;
import com.learningassistant.learninggoal.LearningGoalRepository;
import com.learningassistant.student.Student;
import com.learningassistant.student.StudentRepository;
import com.learningassistant.task.StudyTask;
import com.learningassistant.task.StudyTaskRepository;
import com.learningassistant.task.TaskDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {
    private final StudentRepository students;
    private final LearningGoalRepository goals;
    private final StudyTaskRepository tasks;

    public DashboardService(StudentRepository students, LearningGoalRepository goals, StudyTaskRepository tasks) {
        this.students = students;
        this.goals = goals;
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.DashboardResponse getDashboard(String email) {
        Student student = students.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student"));
        List<StudyTask> allTasks = tasks.findAllByLearningGoal_Student_EmailOrderByDueDateAsc(email);
        LocalDate today = LocalDate.now();
        List<TaskDtos.TaskResponse> dueToday = allTasks.stream()
                .filter(task -> !task.isCompleted() && today.equals(task.getDueDate()))
                .map(TaskDtos.TaskResponse::from).toList();
        List<TaskDtos.TaskResponse> upcoming = allTasks.stream()
                .filter(task -> !task.isCompleted() && task.getDueDate() != null
                        && task.getDueDate().isAfter(today)
                        && !task.getDueDate().isAfter(today.plusDays(7)))
                .map(TaskDtos.TaskResponse::from).toList();
        List<TaskDtos.TaskResponse> recent = allTasks.stream()
                .filter(task -> task.isCompleted() && task.getCompletedAt() != null
                        && task.getCompletedAt().isAfter(java.time.Instant.now().minus(30, ChronoUnit.DAYS)))
                .sorted(Comparator.comparing(StudyTask::getCompletedAt).reversed())
                .limit(5).map(TaskDtos.TaskResponse::from).toList();

        return new DashboardDtos.DashboardResponse(student.getDisplayName(),
                goals.findAllByStudent_EmailAndStatusOrderByCreatedAtDesc(email, GoalStatus.ACTIVE).stream()
                        .map(LearningGoalDtos.GoalResponse::from).toList(),
                allTasks.stream().filter(StudyTask::isCompleted).count(),
                allTasks.stream().filter(task -> !task.isCompleted()).count(),
                dueToday, upcoming, recent);
    }
}
