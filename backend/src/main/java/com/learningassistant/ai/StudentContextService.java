package com.learningassistant.ai;

import com.learningassistant.learninggoal.GoalStatus;
import com.learningassistant.learninggoal.LearningGoalDtos;
import com.learningassistant.learninggoal.LearningGoalRepository;
import com.learningassistant.recommendation.AiRecommendationRepository;
import com.learningassistant.recommendation.AiRecommendationDtos;
import com.learningassistant.student.Student;
import com.learningassistant.student.StudentRepository;
import com.learningassistant.task.StudyTask;
import com.learningassistant.task.StudyTaskRepository;
import com.learningassistant.task.TaskDtos;
import com.learningassistant.lesson.LessonRepository;
import com.learningassistant.lesson.LessonDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds a secure, student-scoped context object for the AI coach.
 *
 * <p>Every query is filtered by the authenticated student's email. No cross-student
 * data is ever loaded. The returned {@link StudentContext} contains only real
 * persisted data — the AI must not fabricate progress or performance.</p>
 */
@Service
public class StudentContextService {

    private static final Logger log = LoggerFactory.getLogger(StudentContextService.class);

    private final StudentRepository students;
    private final LearningGoalRepository goals;
    private final StudyTaskRepository tasks;
    private final LessonRepository lessons;
    private final AiRecommendationRepository recommendations;

    public StudentContextService(StudentRepository students,
                                 LearningGoalRepository goals,
                                 StudyTaskRepository tasks,
                                 LessonRepository lessons,
                                 AiRecommendationRepository recommendations) {
        this.students = students;
        this.goals = goals;
        this.tasks = tasks;
        this.lessons = lessons;
        this.recommendations = recommendations;
    }

    @Transactional(readOnly = true)
    public StudentContext build(String email) {
        Student student = students.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        List<LearningGoalDtos.GoalResponse> activeGoals = goals
                .findAllByStudent_EmailAndStatusOrderByCreatedAtDesc(email, GoalStatus.ACTIVE)
                .stream().map(LearningGoalDtos.GoalResponse::from).toList();

        List<StudyTask> allTasks = tasks.findAllByLearningGoal_Student_EmailOrderByDueDateAsc(email);
        LocalDate today = LocalDate.now();

        List<StudentContext.TaskSummary> pending = allTasks.stream()
                .filter(t -> !t.isCompleted())
                .map(t -> toTaskSummary(t, today))
                .sorted(Comparator.comparing(StudentContext.TaskSummary::dueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(10)
                .toList();

        List<StudentContext.TaskSummary> completed = allTasks.stream()
                .filter(StudyTask::isCompleted)
                .sorted(Comparator.comparing(StudyTask::getCompletedAt).reversed())
                .limit(10)
                .map(t -> toTaskSummary(t, today))
                .toList();

        List<StudentContext.LessonSummary> recentLessons = lessons
                .findAllByLearningGoal_IdAndStudent_EmailOrderByCreatedAtDesc(
                        activeGoals.isEmpty() ? null : activeGoals.get(0).id(), email)
                .stream()
                .limit(5)
                .map(this::toLessonSummary)
                .toList();

        List<StudentContext.RecommendationSummary> recentRecs = recommendations
                .findTopByStudent_EmailOrderByCreatedAtDesc(email)
                .map(AiRecommendationDtos.RecommendationResponse::from)
                .stream()
                .limit(3)
                .map(this::toRecSummary)
                .toList();

        long overdue = pending.stream()
                .filter(StudentContext.TaskSummary::overdue)
                .count();

        return new StudentContext(
                student.getDisplayName(),
                activeGoals,
                pending,
                completed,
                recentLessons,
                recentRecs,
                pending.size(),
                completed.size(),
                overdue
        );
    }

    private StudentContext.TaskSummary toTaskSummary(StudyTask t, LocalDate today) {
        boolean overdue = t.getDueDate() != null
                && !t.isCompleted()
                && t.getDueDate().isBefore(today);
        return new StudentContext.TaskSummary(
                t.getTitle(),
                t.getLearningGoal() != null ? t.getLearningGoal().getSubject() : null,
                t.getLearningGoal() != null ? t.getLearningGoal().getTopic() : null,
                t.getTaskType().name(),
                t.getEstimatedMinutes(),
                t.getDueDate() != null ? t.getDueDate().toString() : null,
                overdue
        );
    }

    private StudentContext.LessonSummary toLessonSummary(LessonDtos.LessonResponse l) {
        return new StudentContext.LessonSummary(
                l.title(), l.topic(), l.difficulty(),
                l.estimatedStudyMinutes(), l.createdAt() != null ? l.createdAt().toString() : null
        );
    }

    private StudentContext.RecommendationSummary toRecSummary(AiRecommendationDtos.RecommendationResponse r) {
        return new StudentContext.RecommendationSummary(
                r.title(), r.recommendation(), r.reason(), r.topic(),
                r.activityType(), r.estimatedMinutes(), r.providerName(), r.fallbackUsed()
        );
    }
}