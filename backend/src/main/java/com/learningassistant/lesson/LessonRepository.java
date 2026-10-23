package com.learningassistant.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<LearningLesson, UUID> {
    List<LearningLesson> findAllByLearningGoal_IdAndStudent_EmailOrderByCreatedAtDesc(
            UUID goalId, String email);

    Optional<LearningLesson> findByIdAndStudent_Email(UUID id, String email);
}
