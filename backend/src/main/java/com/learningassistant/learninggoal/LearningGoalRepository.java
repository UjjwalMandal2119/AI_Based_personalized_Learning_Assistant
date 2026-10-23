package com.learningassistant.learninggoal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningGoalRepository extends JpaRepository<LearningGoal, UUID> {
    List<LearningGoal> findAllByStudent_EmailOrderByCreatedAtDesc(String email);
    List<LearningGoal> findAllByStudent_EmailAndStatusOrderByCreatedAtDesc(String email, GoalStatus status);
    Optional<LearningGoal> findByIdAndStudent_Email(UUID id, String email);
}
