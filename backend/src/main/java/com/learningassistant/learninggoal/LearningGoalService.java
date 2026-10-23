package com.learningassistant.learninggoal;

import com.learningassistant.common.ResourceNotFoundException;
import com.learningassistant.student.Student;
import com.learningassistant.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LearningGoalService {
    private final LearningGoalRepository goals;
    private final StudentRepository students;

    public LearningGoalService(LearningGoalRepository goals, StudentRepository students) {
        this.goals = goals;
        this.students = students;
    }

    @Transactional
    public LearningGoalDtos.GoalResponse create(String email, LearningGoalDtos.GoalRequest request) {
        Student student = students.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student"));
        LearningGoal goal = goals.save(new LearningGoal(student, request.title().trim(), request.subject().trim(),
                request.topic().trim(), request.proficiencyLevel(), request.targetDate()));
        return LearningGoalDtos.GoalResponse.from(goal);
    }

    @Transactional(readOnly = true)
    public List<LearningGoalDtos.GoalResponse> list(String email) {
        return goals.findAllByStudent_EmailOrderByCreatedAtDesc(email).stream()
                .map(LearningGoalDtos.GoalResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LearningGoalDtos.GoalResponse get(String email, UUID id) {
        return LearningGoalDtos.GoalResponse.from(ownedGoal(email, id));
    }

    @Transactional
    public LearningGoalDtos.GoalResponse update(String email, UUID id, LearningGoalDtos.GoalRequest request) {
        LearningGoal goal = ownedGoal(email, id);
        goal.update(request.title().trim(), request.subject().trim(), request.topic().trim(),
                request.proficiencyLevel(), request.targetDate(),
                request.status() == null ? goal.getStatus() : request.status());
        return LearningGoalDtos.GoalResponse.from(goal);
    }

    @Transactional
    public void delete(String email, UUID id) {
        goals.delete(ownedGoal(email, id));
    }

    public LearningGoal ownedGoal(String email, UUID id) {
        return goals.findByIdAndStudent_Email(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Learning goal"));
    }
}
