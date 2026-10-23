package com.learningassistant.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudyTaskRepository extends JpaRepository<StudyTask, UUID> {
    List<StudyTask> findAllByLearningGoal_IdAndLearningGoal_Student_EmailOrderByDueDateAsc(
            UUID goalId, String email);
    List<StudyTask> findAllByLearningGoal_Student_EmailOrderByDueDateAsc(String email);
    Optional<StudyTask> findByIdAndLearningGoal_Student_Email(UUID id, String email);
}
