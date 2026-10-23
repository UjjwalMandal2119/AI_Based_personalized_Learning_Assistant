package com.learningassistant.lesson;

import java.util.List;

public record LessonContent(
        List<String> learningObjectives,
        List<LessonSection> sections,
        List<LessonExample> examples,
        List<String> keyTakeaways) {

    public record LessonSection(String heading, String explanation) {}

    public record LessonExample(String title, String scenario, String solution) {}
}
