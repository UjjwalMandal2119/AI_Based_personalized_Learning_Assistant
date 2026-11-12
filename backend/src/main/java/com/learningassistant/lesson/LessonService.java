package com.learningassistant.lesson;

import com.learningassistant.common.ResourceNotFoundException;
import com.learningassistant.learninggoal.LearningGoal;
import com.learningassistant.learninggoal.LearningGoalService;
import com.learningassistant.student.Student;
import com.learningassistant.student.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LessonService {

    private static final Logger log = LoggerFactory.getLogger(LessonService.class);

    private final LearningGoalService goals;
    private final StudentRepository students;
    private final LessonRepository lessons;
    private final OllamaLessonClient ollamaClient;
    private final GeminiApiClient geminiClient;
    private final LessonContentValidator validator;
    private final boolean fallbackEnabled;

    public LessonService(LearningGoalService goals, StudentRepository students,
                         LessonRepository lessons, OllamaLessonClient ollamaClient,
                         GeminiApiClient geminiClient, LessonContentValidator validator,
                         @Value("${ai.fallback.enabled:true}") boolean fallbackEnabled) {
        this.goals = goals;
        this.students = students;
        this.lessons = lessons;
        this.ollamaClient = ollamaClient;
        this.geminiClient = geminiClient;
        this.validator = validator;
        this.fallbackEnabled = fallbackEnabled;
    }

    @Transactional
    public LessonDtos.LessonResponse generate(String email, UUID goalId) {
        LearningGoal goal = goals.ownedGoal(email, goalId);
        Student student = students.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student"));

        GeminiApiClient.GeneratedLesson generated = generateWithFallback(goal);
        if (generated == null) {
            throw new LessonGenerationException(HttpStatus.BAD_GATEWAY,
                    "The AI provider returned lesson content in an invalid format. Please try again.");
        }
        validator.validate(generated.title(), generated.content(), generated.estimatedStudyMinutes());
        LearningLesson lesson = lessons.save(new LearningLesson(goal, student,
                generated.title().trim(), goal.getTopic(),
                goal.getProficiencyLevel().name(), generated.content(),
                generated.estimatedStudyMinutes()));
        return LessonDtos.LessonResponse.from(lesson);
    }

    private GeminiApiClient.GeneratedLesson generateWithFallback(LearningGoal goal) {
        if (ollamaClient.isAvailable()) {
            try {
                GeminiApiClient.GeneratedLesson result = ollamaClient.generate(goal);
                log.info("Lesson generated via Ollama for goal '{}'", goal.getTopic());
                return result;
            } catch (LessonGenerationException e) {
                log.warn("Ollama lesson generation failed: {} — trying Gemini fallback", e.getMessage());
                if (!fallbackEnabled) throw e;
            }
        } else {
            log.info("Ollama unavailable for lesson generation — using Gemini");
        }
        return geminiClient.generate(goal);
    }

    @Transactional(readOnly = true)
    public List<LessonDtos.LessonResponse> list(String email, UUID goalId) {
        goals.ownedGoal(email, goalId);
        return lessons.findAllByLearningGoal_IdAndStudent_EmailOrderByCreatedAtDesc(goalId, email)
                .stream().map(LessonDtos.LessonResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LessonDtos.LessonResponse get(String email, UUID lessonId) {
        return lessons.findByIdAndStudent_Email(lessonId, email)
                .map(LessonDtos.LessonResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson"));
    }
}
