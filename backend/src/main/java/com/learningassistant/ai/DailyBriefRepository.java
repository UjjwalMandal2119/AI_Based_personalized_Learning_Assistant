package com.learningassistant.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DailyBriefRepository extends JpaRepository<DailyBrief, UUID> {
    Optional<DailyBrief> findByStudent_EmailAndBriefDate(String email, LocalDate date);
    Optional<DailyBrief> findTopByStudent_EmailOrderByBriefDateDesc(String email);
}