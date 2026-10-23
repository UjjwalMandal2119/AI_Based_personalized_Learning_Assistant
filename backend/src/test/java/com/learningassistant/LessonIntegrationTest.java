package com.learningassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningassistant.lesson.GeminiApiClient;
import com.learningassistant.lesson.LessonContent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LessonIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean GeminiApiClient gemini;

    @Test
    void generatesPersistsAndScopesLessonsToTheStudent() throws Exception {
        String token = register("lesson-student@example.com");
        String goalId = createGoal(token);
        given(gemini.generate(org.mockito.ArgumentMatchers.any()))
                .willReturn(validGeneratedLesson());

        String response = mvc.perform(post("/api/learning-goals/" + goalId + "/lessons/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Fractions made clear"))
                .andExpect(jsonPath("$.topic").value("Fractions"))
                .andExpect(jsonPath("$.content.learningObjectives[0]").value("Compare fractions"))
                .andReturn().getResponse().getContentAsString();
        String lessonId = objectMapper.readTree(response).get("id").asText();

        mvc.perform(get("/api/learning-goals/" + goalId + "/lessons")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(lessonId));
        mvc.perform(get("/api/lessons/" + lessonId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.sections[0].heading").value("Equivalent fractions"))
                .andExpect(jsonPath("$.estimatedStudyMinutes").value(20));

        String otherToken = register("other-lesson-student@example.com");
        mvc.perform(get("/api/lessons/" + lessonId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/learning-goals/" + goalId + "/lessons")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        verify(gemini).generate(org.mockito.ArgumentMatchers.argThat(goal ->
                goal.getTopic().equals("Fractions") && goal.getProficiencyLevel().name().equals("BEGINNER")));
    }

    @Test
    void doesNotPersistMalformedGeneratedLesson() throws Exception {
        String token = register("invalid-lesson@example.com");
        String goalId = createGoal(token);
        given(gemini.generate(org.mockito.ArgumentMatchers.any()))
                .willReturn(new GeminiApiClient.GeneratedLesson(
                        "Lesson title", List.of(), List.of(), List.of(), List.of(), 25));

        mvc.perform(post("/api/learning-goals/" + goalId + "/lessons/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Lesson must contain between 1 and 8 learning objectives"));
        mvc.perform(get("/api/learning-goals/" + goalId + "/lessons")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private GeminiApiClient.GeneratedLesson validGeneratedLesson() {
        return new GeminiApiClient.GeneratedLesson("Fractions made clear",
                List.of("Compare fractions"),
                List.of(new LessonContent.LessonSection("Equivalent fractions",
                        "Multiply or divide the numerator and denominator by the same number.")),
                List.of(new LessonContent.LessonExample("Compare one half and two fourths",
                        "Are 1/2 and 2/4 equal?", "Multiply 1/2 by 2/2 to get 2/4.")),
                List.of("Equivalent fractions have the same value."), 20);
    }

    private String register(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Registration("Lesson Student", email, "correct-horse-4"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String createGoal(String token) throws Exception {
        String response = mvc.perform(post("/api/learning-goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Learn fractions","subject":"Math","topic":"Fractions",
                                 "proficiencyLevel":"BEGINNER"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private record Registration(String displayName, String email, String password) {}
}
