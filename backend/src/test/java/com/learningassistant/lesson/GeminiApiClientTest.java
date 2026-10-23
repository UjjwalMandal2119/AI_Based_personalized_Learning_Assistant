package com.learningassistant.lesson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningassistant.learninggoal.LearningGoal;
import com.learningassistant.learninggoal.ProficiencyLevel;
import com.learningassistant.student.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiApiClientTest {
    private MockRestServiceServer server;
    private GeminiApiClient client;
    private LearningGoal goal;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiApiClient(builder.build(), new ObjectMapper(), "test-key", "gemini-3.8-flash");
        goal = new LearningGoal(new Student("student@example.com", "hash", "Student"),
                "Learn algebra", "Math", "Linear equations", ProficiencyLevel.BEGINNER, null);
    }

    @Test
    void sendsStructuredRequestAndParsesValidLesson() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withSuccess(response("""
                        {"title":"Linear equations","learningObjectives":["Solve for an unknown"],
                         "sections":[{"heading":"Balance both sides","explanation":"Apply the same operation to each side."}],
                         "examples":[{"title":"A simple equation","scenario":"x + 2 = 5","solution":"Subtract 2 from both sides, so x = 3."}],
                         "keyTakeaways":["Keep both sides balanced"],"estimatedStudyMinutes":20}
                        """), MediaType.APPLICATION_JSON));

        GeminiApiClient.GeneratedLesson generated = client.generate(goal);

        assertThat(generated.title()).isEqualTo("Linear equations");
        assertThat(generated.content().sections()).hasSize(1);
        assertThat(generated.content().examples().getFirst().solution()).contains("x = 3");
        assertThat(generated.estimatedStudyMinutes()).isEqualTo(20);
        server.verify();
    }

    @Test
    void rejectsMalformedProviderJson() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(withSuccess(response("not-json"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate(goal))
                .isInstanceOf(LessonGenerationException.class)
                .hasMessageContaining("invalid format");
        server.verify();
    }

    @Test
    void mapsProviderRateLimitTo429() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.generate(goal))
                .isInstanceOf(LessonGenerationException.class)
                .satisfies(exception -> assertThat(((LessonGenerationException) exception).getStatus())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        server.verify();
    }

    private String response(String text) {
        try {
            return new ObjectMapper().writeValueAsString(java.util.Map.of(
                    "candidates", java.util.List.of(java.util.Map.of(
                            "content", java.util.Map.of("parts", java.util.List.of(java.util.Map.of("text", text)))))));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
