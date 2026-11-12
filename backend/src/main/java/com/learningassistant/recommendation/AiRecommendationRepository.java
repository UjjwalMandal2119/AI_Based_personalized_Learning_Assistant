package com.learningassistant.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, UUID> {
    Optional<AiRecommendation> findTopByStudent_EmailOrderByCreatedAtDesc(String email);
}
