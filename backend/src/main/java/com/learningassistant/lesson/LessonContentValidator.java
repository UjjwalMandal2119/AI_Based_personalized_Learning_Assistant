package com.learningassistant.lesson;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LessonContentValidator {
    private static final int MAX_OBJECTIVES = 8;
    private static final int MAX_SECTIONS = 10;
    private static final int MAX_EXAMPLES = 8;
    private static final int MAX_TAKEAWAYS = 10;

    public void validate(String title, LessonContent content, int estimatedMinutes) {
        requireText(title, 160, "title");
        if (content == null) {
            throw invalid("Lesson content is missing");
        }
        validateTexts(content.learningObjectives(), MAX_OBJECTIVES, 300, "learning objectives");
        if (content.sections() == null || content.sections().isEmpty()
                || content.sections().size() > MAX_SECTIONS) {
            throw invalid("Lesson must contain between 1 and " + MAX_SECTIONS + " sections");
        }
        for (LessonContent.LessonSection section : content.sections()) {
            if (section == null) throw invalid("Lesson section is incomplete");
            requireText(section.heading(), 120, "section heading");
            requireText(section.explanation(), 5000, "section explanation");
        }
        if (content.examples() == null || content.examples().size() > MAX_EXAMPLES) {
            throw invalid("Lesson contains too many examples");
        }
        for (LessonContent.LessonExample example : content.examples()) {
            if (example == null) throw invalid("Lesson example is incomplete");
            requireText(example.title(), 120, "example title");
            requireText(example.scenario(), 2000, "example scenario");
            requireText(example.solution(), 5000, "example solution");
        }
        validateTexts(content.keyTakeaways(), MAX_TAKEAWAYS, 500, "key takeaways");
        if (estimatedMinutes < 1 || estimatedMinutes > 240) {
            throw invalid("Estimated study time must be between 1 and 240 minutes");
        }
    }

    private void validateTexts(List<String> values, int maxCount, int maxLength, String field) {
        if (values == null || values.isEmpty() || values.size() > maxCount) {
            throw invalid("Lesson must contain between 1 and " + maxCount + " " + field);
        }
        values.forEach(value -> requireText(value, maxLength, field));
    }

    private void requireText(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw invalid("Lesson " + field + " is missing or too long");
        }
    }

    private LessonGenerationException invalid(String message) {
        return new LessonGenerationException(org.springframework.http.HttpStatus.BAD_GATEWAY, message);
    }
}
