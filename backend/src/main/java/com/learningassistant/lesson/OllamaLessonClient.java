package com.learningassistant.lesson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningassistant.ai.AiProviderException;
import com.learningassistant.ai.OllamaAiProvider;
import com.learningassistant.learninggoal.LearningGoal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Generates structured lessons using Ollama via prompt-engineered JSON output.
 * Falls back to {@link GeminiApiClient} when Ollama is unavailable.
 */
@Component
public class OllamaLessonClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaLessonClient.class);

    private final OllamaAiProvider ollama;
    private final ObjectMapper objectMapper;

    public OllamaLessonClient(OllamaAiProvider ollama, ObjectMapper objectMapper) {
        this.ollama = ollama;
        this.objectMapper = objectMapper;
    }

    public boolean isAvailable() {
        return ollama.isAvailable();
    }

    /**
     * Generates a lesson. Returns the parsed lesson or throws {@link LessonGenerationException}.
     */
    public GeminiApiClient.GeneratedLesson generate(LearningGoal goal) {
        String prompt = buildPrompt(goal);
        String raw;
        try {
            raw = ollama.generateText(prompt);
        } catch (AiProviderException e) {
            throw new LessonGenerationException(e.getStatus(), e.getMessage(), e);
        }
        return parse(raw);
    }

    private GeminiApiClient.GeneratedLesson parse(String raw) {
        String clean = raw.trim();
        if (clean.startsWith("```")) {
            clean = clean.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }
        // Extract first JSON object if model adds surrounding text
        int start = clean.indexOf('{');
        int end = clean.lastIndexOf('}');
        if (start >= 0 && end > start) {
            clean = clean.substring(start, end + 1);
        }
        try {
            return objectMapper.readValue(clean, GeminiApiClient.GeneratedLesson.class);
        } catch (JsonProcessingException e) {
            log.warn("Ollama lesson JSON parse failed: {}", e.getMessage());
            throw new LessonGenerationException(HttpStatus.BAD_GATEWAY,
                    "Ollama returned lesson content in an invalid format. Please try again.", e);
        }
    }

    private String buildPrompt(LearningGoal goal) {
        return """
                Create a clear, accurate study lesson for a student.
                Topic: %s
                Subject: %s
                Current proficiency: %s
                Learning goal: %s

                Respond with ONLY a valid JSON object — no markdown, no explanation, no code fences.
                The JSON must have exactly these fields:
                {
                  "title": "string",
                  "learningObjectives": ["string", ...],
                  "sections": [{"heading": "string", "explanation": "string"}, ...],
                  "examples": [{"title": "string", "scenario": "string", "solution": "string"}, ...],
                  "keyTakeaways": ["string", ...],
                  "estimatedStudyMinutes": integer
                }
                Include 1-4 learning objectives, 2-5 sections, 1-3 examples, 2-5 takeaways.
                Match depth and vocabulary to the stated proficiency level.
                """.formatted(goal.getTopic(), goal.getSubject(),
                goal.getProficiencyLevel(), goal.getTitle());
    }
}
