package com.learningassistant.lesson;

import com.learningassistant.common.ResourceNotFoundException;
import com.learningassistant.learninggoal.LearningGoal;
import com.learningassistant.learninggoal.LearningGoalService;
import com.learningassistant.student.Student;
import com.learningassistant.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LessonService {
    private final LearningGoalService goals;
    private final StudentRepository students;
    private final LessonRepository lessons;
    private final GeminiApiClient gemini;
    private final LessonContentValidator validator;

    public LessonService(LearningGoalService goals, StudentRepository students, LessonRepository lessons,
                         GeminiApiClient gemini, LessonContentValidator validator) {
        this.goals = goals;
        this.students = students;
        this.lessons = lessons;
        this.gemini = gemini;
        this.validator = validator;
    }

    public LessonDtos.LessonResponse generate(String email, UUID goalId) {
        LearningGoal goal = goals.ownedGoal(email, goalId);
        Student student = students.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student"));
        GeminiApiClient.GeneratedLesson generated = gemini.generate(goal);
        if (generated == null) {
            throw new LessonGenerationException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Gemini returned lesson content in an invalid format. Please try again.");
        }
        validator.validate(generated.title(), generated.content(), generated.estimatedStudyMinutes());
        LearningLesson lesson = lessons.save(new LearningLesson(goal, student, generated.title().trim(),
                goal.getTopic(), goal.getProficiencyLevel().name(), generated.content(),
                generated.estimatedStudyMinutes()));
        return LessonDtos.LessonResponse.from(lesson);
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
