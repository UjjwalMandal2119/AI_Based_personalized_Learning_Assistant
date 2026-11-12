package com.learningassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningassistant.ai.AiGenerationResult;
import com.learningassistant.ai.AiProviderException;
import com.learningassistant.ai.AiProviderRouter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiRecommendationIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AiProviderRouter aiRouter;

    private static final String VALID_JSON = """
            {"title":"Study algebra","recommendation":"Practice daily",
             "reason":"You have pending tasks","topic":"Algebra",
             "activityType":"PRACTICE","estimatedMinutes":30,
             "suggestedActions":["Solve 5 equations"]}
            """;

    @Test
    void ollamaSuccessReturnsRecommendationWithProviderName() throws Exception {
        String token = register("rec-ollama@example.com");
        createGoal(token);
        given(aiRouter.generate(anyString())).willReturn(
                new AiGenerationResult(VALID_JSON, "ollama", "llama3.2:3b", 1200L, false));

        mvc.perform(post("/api/ai/recommendations/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Study algebra"))
                .andExpect(jsonPath("$.providerName").value("ollama"))
                .andExpect(jsonPath("$.fallbackUsed").value(false));
    }

    @Test
    void ollamaFailureFallsBackToGemini() throws Exception {
        String token = register("rec-fallback@example.com");
        createGoal(token);
        given(aiRouter.generate(anyString())).willReturn(
                new AiGenerationResult(VALID_JSON, "gemini", "gemini-3.8-flash", 3500L, true));

        mvc.perform(post("/api/ai/recommendations/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerName").value("gemini"))
                .andExpect(jsonPath("$.fallbackUsed").value(true));
    }

    @Test
    void bothProvidersFailReturns204() throws Exception {
        String token = register("rec-both-fail@example.com");
        createGoal(token);
        given(aiRouter.generate(anyString())).willThrow(
                new AiProviderException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Both providers failed", false));

        mvc.perform(post("/api/ai/recommendations/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void noGoalsReturns204() throws Exception {
        String token = register("rec-no-goals@example.com");

        mvc.perform(post("/api/ai/recommendations/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void cachedRecommendationReturnedOnGetWithoutCallingAi() throws Exception {
        String token = register("rec-cache@example.com");
        createGoal(token);
        given(aiRouter.generate(anyString())).willReturn(
                new AiGenerationResult(VALID_JSON, "ollama", "llama3.2:3b", 900L, false));

        // First call generates
        mvc.perform(post("/api/ai/recommendations/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // GET returns cached without calling AI again
        mvc.perform(get("/api/ai/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Study algebra"));
    }

    @Test
    void sixHourCacheShortCircuitsRouterOnSecondPostGenerate() throws Exception {
        // Verifies that getOrGenerate returns the cached recommendation within 6 hours
        // and does NOT call the AI router a second time.
        String token = register("rec-cache-post@example.com");
        createGoal(token);
        given(aiRouter.generate(anyString())).willReturn(
                new AiGenerationResult(VALID_JSON, "ollama", "llama3.2:3b", 900L, false));

        // First POST — generates and persists
        mvc.perform(post("/api/ai/recommendations/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerName").value("ollama"));

        // Second POST within 6 hours — must return cached, router must NOT be called again
        mvc.perform(post("/api/ai/recommendations/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Study algebra"));

        verify(aiRouter, times(1)).generate(anyString());
    }

    @Test
    void providerMetadataIsPersistedAndReturnedInResponse() throws Exception {
        String token = register("rec-metadata@example.com");
        createGoal(token);
        given(aiRouter.generate(anyString())).willReturn(
                new AiGenerationResult(VALID_JSON, "ollama", "llama3.2:3b", 1450L, false));

        mvc.perform(post("/api/ai/recommendations/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerName").value("ollama"))
                .andExpect(jsonPath("$.fallbackUsed").value(false))
                .andExpect(jsonPath("$.learningGoalId").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(post("/api/ai/recommendations/generate"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/ai/recommendations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crossStudentIsolation() throws Exception {
        String tokenA = register("rec-student-a@example.com");
        String tokenB = register("rec-student-b@example.com");
        createGoal(tokenA);
        given(aiRouter.generate(anyString())).willReturn(
                new AiGenerationResult(VALID_JSON, "ollama", "llama3.2:3b", 800L, false));

        mvc.perform(post("/api/ai/recommendations/generate")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Student B has no recommendation
        mvc.perform(get("/api/ai/recommendations")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNoContent());
    }

    private String register(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Registration("Test Student", email, "correct-horse-4"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private void createGoal(String token) throws Exception {
        mvc.perform(post("/api/learning-goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Learn algebra","subject":"Math","topic":"Algebra",
                                 "proficiencyLevel":"BEGINNER"}
                                """))
                .andExpect(status().isCreated());
    }

    private record Registration(String displayName, String email, String password) {}
}
