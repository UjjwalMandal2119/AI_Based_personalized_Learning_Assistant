package com.learningassistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningassistant.student.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VerticalSliceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired StudentRepository students;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void registrationLoginGoalTasksAndDashboardStayScopedToStudent() throws Exception {
        String firstToken = register("ada@example.com", "Ada Learner");
        assertThat(passwordEncoder.matches("correct-horse-4", students.findByEmail("ada@example.com")
                .orElseThrow().getPasswordHash())).isTrue();
        String loginToken = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ada@example.com","password":"correct-horse-4"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginToken).get("token").asText();

        String goalResponse = mvc.perform(post("/api/learning-goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Learn algebra","subject":"Math","topic":"Linear equations",
                                 "proficiencyLevel":"BEGINNER","targetDate":"%s"}
                                """.formatted(LocalDate.now().plusDays(10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String goalId = objectMapper.readTree(goalResponse).get("id").asText();

        String taskResponse = mvc.perform(post("/api/learning-goals/" + goalId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Practice equations","description":"Solve five problems",
                                 "taskType":"PRACTICE","estimatedMinutes":25,"dueDate":"%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completed").value(false))
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(taskResponse).get("id").asText();

        mvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ada Learner"))
                .andExpect(jsonPath("$.activeGoals.length()").value(1))
                .andExpect(jsonPath("$.remainingTasks").value(1))
                .andExpect(jsonPath("$.tasksDueToday.length()").value(1));

        mvc.perform(patch("/api/tasks/" + taskId + "/completion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        mvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedTasks").value(1))
                .andExpect(jsonPath("$.remainingTasks").value(0))
                .andExpect(jsonPath("$.recentActivity.length()").value(1));

        String anotherStudentToken = register("grace@example.com", "Grace Learner");
        mvc.perform(get("/api/learning-goals/" + goalId)
                        .header("Authorization", "Bearer " + anotherStudentToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
        assertThat(firstToken).isNotBlank();
    }

    @Test
    void rejectsInvalidRegistrationAndBadCredentials() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"","email":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest());

        register("student@example.com", "Student");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"student@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private String register(String email, String displayName) throws Exception {
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Registration(displayName, email, "correct-horse-4"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private record Registration(String displayName, String email, String password) {}
}
