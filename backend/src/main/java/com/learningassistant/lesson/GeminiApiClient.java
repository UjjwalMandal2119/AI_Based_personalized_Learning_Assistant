package com.learningassistant.lesson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningassistant.learninggoal.LearningGoal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class GeminiApiClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiApiClient(RestClient geminiRestClient, ObjectMapper objectMapper,
                           @Value("${gemini.api-key:}") String apiKey,
                           @Value("${gemini.model:gemini-3.8-flash}") String model) {
        this.restClient = geminiRestClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public GeneratedLesson generate(LearningGoal goal) {
        if (apiKey.isBlank()) {
            throw new LessonGenerationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Lesson generation is not configured. Set GEMINI_API_KEY on the backend.");
        }

        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", promptFor(goal))))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema(),
                        "maxOutputTokens", 8192,
                        "temperature", 0.4));
        try {
            JsonNode response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            return parseGeneratedLesson(response);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                throw new LessonGenerationException(HttpStatus.TOO_MANY_REQUESTS,
                        "Gemini is rate-limiting lesson requests. Please try again shortly.", exception);
            }
            throw new LessonGenerationException(HttpStatus.BAD_GATEWAY,
                    "The lesson provider could not complete this request.", exception);
        } catch (ResourceAccessException exception) {
            throw new LessonGenerationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The lesson provider is temporarily unavailable. Please try again shortly.", exception);
        }
    }

    GeneratedLesson parseGeneratedLesson(JsonNode response) {
        JsonNode textNode = response == null ? null
                : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode == null || !textNode.isTextual() || textNode.asText().isBlank()) {
            throw malformed(null);
        }
        try {
            return objectMapper.readValue(textNode.asText(), GeneratedLesson.class);
        } catch (JsonProcessingException exception) {
            throw malformed(exception);
        }
    }

    private LessonGenerationException malformed(Throwable cause) {
        return new LessonGenerationException(HttpStatus.BAD_GATEWAY,
                "Gemini returned lesson content in an invalid format. Please try again.", cause);
    }

    private String promptFor(LearningGoal goal) {
        return """
                Create a clear, accurate study lesson for a student.
                Topic: %s
                Subject: %s
                Current proficiency: %s
                Learning goal: %s

                Match the depth, vocabulary, and examples to the stated proficiency.
                Explain concepts in your own words. Do not include HTML, scripts, or markdown links.
                Use worked examples where they help teach the topic.
                Return only the JSON object required by the response schema.
                """.formatted(goal.getTopic(), goal.getSubject(), goal.getProficiencyLevel(), goal.getTitle());
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> section = objectSchema(
                Map.of("heading", stringSchema(), "explanation", stringSchema()),
                List.of("heading", "explanation"));
        Map<String, Object> example = objectSchema(
                Map.of("title", stringSchema(), "scenario", stringSchema(), "solution", stringSchema()),
                List.of("title", "scenario", "solution"));
        return objectSchema(Map.of(
                "title", stringSchema(),
                "learningObjectives", arraySchema(stringSchema()),
                "sections", arraySchema(section),
                "examples", arraySchema(example),
                "keyTakeaways", arraySchema(stringSchema()),
                "estimatedStudyMinutes", Map.of("type", "INTEGER")),
                List.of("title", "learningObjectives", "sections", "examples",
                        "keyTakeaways", "estimatedStudyMinutes"));
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        return Map.of("type", "OBJECT", "properties", properties, "required", required);
    }

    private Map<String, Object> arraySchema(Map<String, Object> items) {
        return Map.of("type", "ARRAY", "items", items);
    }

    private Map<String, Object> stringSchema() {
        return Map.of("type", "STRING");
    }

    public record GeneratedLesson(
            String title,
            List<String> learningObjectives,
            List<LessonContent.LessonSection> sections,
            List<LessonContent.LessonExample> examples,
            List<String> keyTakeaways,
            int estimatedStudyMinutes) {
        public LessonContent content() {
            return new LessonContent(learningObjectives, sections, examples, keyTakeaways);
        }
    }
}
