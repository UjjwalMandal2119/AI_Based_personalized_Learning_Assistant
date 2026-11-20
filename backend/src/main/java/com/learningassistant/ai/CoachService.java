package com.learningassistant.ai;

import com.learningassistant.ai.AiProviderRouter;
import com.learningassistant.ai.StudentContext;
import com.learningassistant.ai.StudentContextService;
import com.learningassistant.chat.ChatMessage;
import com.learningassistant.chat.Conversation;
import com.learningassistant.chat.ConversationRepository;
import com.learningassistant.chat.ChatMessageRepository;
import com.learningassistant.chat.ChatDtos;
import com.learningassistant.common.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Main AI Coach service providing structured, personalized learning assistance.
 *
 * <p>Supports six interaction modes: LEARN, PRACTICE, QUIZ, PLAN, INTERVIEW, IMPROVE.
 * Every response is structured with follow-up suggestions and actionable steps.
 * All interactions are persisted in conversation history.</p>
 */
@Service
public class CoachService {

    private static final Logger log = LoggerFactory.getLogger(CoachService.class);

    private final AiProviderRouter aiRouter;
    private final StudentContextService contextService;
    private final ConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final ChatService chatService;

    public CoachService(AiProviderRouter aiRouter,
                        StudentContextService contextService,
                        ConversationRepository conversations,
                        ChatMessageRepository messages,
                        ChatService chatService) {
        this.aiRouter = aiRouter;
        this.contextService = contextService;
        this.conversations = conversations;
        this.messages = messages;
        this.chatService = chatService;
    }

    /**
     * Process a user message and return a structured coach response.
     */
    @Transactional
    public CoachResponse respond(String email, CoachResponse.CoachRequest request) {
        String conversationId;
        Conversation conversation;

        if (request.conversationId() != null && !request.conversationId().isBlank()) {
            conversation = conversations.findByIdAndStudent_Email(UUID.fromString(request.conversationId()), email)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation"));
            conversationId = request.conversationId();
        } else {
            String title = deriveTitle(request.message());
            String subject = extractSubject(request.message());
            String topic = extractTopic(request.message());
            conversation = conversations.save(new Conversation(
                    chatService.getStudent(email), title, subject, topic));
            conversationId = conversation.getId().toString();
        }

        String mode = request.mode() != null ? request.mode().toUpperCase() : "LEARN";
        String systemPrompt = buildSystemPrompt(mode, contextService.build(email));
        String fullPrompt = buildFullPrompt(systemPrompt, request.history(), request.message());

        AiGenerationResult result;
        try {
            result = aiRouter.generate(fullPrompt);
        } catch (Exception e) {
            log.warn("AI generation failed: {}", e.getMessage());
            return fallbackResponse(conversationId, mode, request.message());
        }

        String reply = result.text();
        List<String> suggestedQuestions = extractSuggestedQuestions(reply, mode);
        List<CoachResponse.CoachAction> actions = extractActions(reply, mode, conversationId);

        messages.save(new ChatMessage(conversation, "user", request.message(), null, null));
        messages.save(new ChatMessage(conversation, "assistant", reply, result.providerName(), result.modelName()));

        return new CoachResponse(
                conversationId,
                reply,
                mode,
                suggestedQuestions,
                actions,
                new CoachResponse.CoachMetadata(
                        result.providerName(),
                        result.modelName(),
                        result.durationMs(),
                        result.fallbackUsed()
                )
        );
    }

    /**
     * Get conversation history for the authenticated student.
     */
    @Transactional(readOnly = true)
    public List<ChatDtos.ConversationSummary> listConversations(String email) {
        return chatService.list(email);
    }

    /**
     * Get full conversation detail with messages.
     */
    @Transactional(readOnly = true)
    public ChatDtos.ConversationDetail getConversation(String email, String conversationId) {
        return chatService.get(email, UUID.fromString(conversationId));
    }

    /**
     * Create a new conversation.
     */
    @Transactional
    public ChatDtos.ConversationDetail createConversation(String email, CoachResponse.CoachRequest request) {
        String title = deriveTitle(request.message());
        String subject = extractSubject(request.message());
        String topic = extractTopic(request.message());
        return chatService.create(email, new ChatDtos.CreateConversationRequest(title, subject, topic));
    }

    /**
     * Delete a conversation.
     */
    @Transactional
    public void deleteConversation(String email, String conversationId) {
        Conversation conv = conversations.findByIdAndStudent_Email(UUID.fromString(conversationId), email)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation"));
        messages.deleteAllByConversation_Id(conv.getId());
        conversations.delete(conv);
    }

    private String buildSystemPrompt(String mode, StudentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are Disha, a patient and supportive AI Learning Coach. ");
        sb.append("Your goal is to teach, guide, and help students learn effectively.\n\n");

        sb.append("STUDENT CONTEXT:\n");
        sb.append("- Name: ").append(context.displayName()).append("\n");
        if (context.hasActiveGoals()) {
            sb.append("- Active Goals:\n");
            for (var g : context.activeGoals()) {
                sb.append("  * ").append(g.title()).append(" (").append(g.subject())
                  .append(", ").append(g.topic()).append(", ").append(g.proficiencyLevel()).append(")\n");
            }
        } else {
            sb.append("- No active goals yet\n");
        }
        if (context.hasPendingTasks()) {
            sb.append("- Pending Tasks (").append(context.pendingCount()).append("):\n");
            for (var t : context.pendingTasks()) {
                sb.append("  * ").append(t.title()).append(" [").append(t.taskType())
                  .append(", ").append(t.estimatedMinutes()).append(" min");
                if (t.dueDate() != null) sb.append(", due ").append(t.dueDate());
                if (t.overdue()) sb.append(" ⚠ OVERDUE");
                sb.append("]\n");
            }
        }
        if (context.hasCompletedTasks()) {
            sb.append("- Recently Completed (").append(context.completedCount()).append("):\n");
            for (var t : context.completedTasks().stream().limit(5).toList()) {
                sb.append("  * ").append(t.title()).append("\n");
            }
        }
        if (context.hasRecentLessons()) {
            sb.append("- Recent Lessons:\n");
            for (var l : context.recentLessons()) {
                sb.append("  * ").append(l.title()).append(" (").append(l.topic()).append(")\n");
            }
        }
        sb.append("\n");

        sb.append("CURRENT MODE: ").append(mode).append("\n\n");
        sb.append(getModeInstructions(mode));
        sb.append("\n\nRESPONSE FORMAT:\n");
        sb.append("Provide a clear, well-structured response. Include:\n");
        sb.append("1. Main explanation/guidance\n");
        sb.append("2. SUGGESTED QUESTIONS: (list 3-5 follow-up questions the student might ask)\n");
        sb.append("3. ACTIONS: (list actionable steps with type, label, payload)\n");
        sb.append("Action types: START_PRACTICE, CREATE_TASK, OPEN_GOAL, GENERATE_LESSON, TAKE_QUIZ, CREATE_PLAN\n");
        return sb.toString();
    }

    private String getModeInstructions(String mode) {
        return switch (mode) {
            case "LEARN" -> """
                MODE: LEARN - Explain concepts step by step.
                - Start by gauging the student's current understanding if unclear
                - Break complex topics into smaller, digestible parts
                - Use simple language, analogies, and practical examples
                - Include code examples for programming topics
                - Check understanding with a simple question at the end
                - Offer to go deeper or provide practice problems
                """;
            case "PRACTICE" -> """
                MODE: PRACTICE - Generate practice questions and evaluate answers.
                - Ask ONE question at a time
                - Wait for student's answer before revealing correct answer
                - Explain why the answer is correct/incorrect
                - Adapt difficulty based on student's responses
                - Track score and provide summary at the end
                - Offer more practice or move to quiz mode
                """;
            case "QUIZ" -> """
                MODE: QUIZ - Conduct interactive quizzes with scoring.
                - Present one question at a time (multiple choice or short answer)
                - Evaluate immediately and explain
                - Keep running score
                - At end: show score, identify weak areas, suggest improvement
                - Offer to retry wrong questions or generate new quiz
                """;
            case "PLAN" -> """
                MODE: PLAN - Create study schedules and learning plans.
                - Ask about goal, deadline, available time, current level if missing
                - Break goal into manageable weekly/daily tasks
                - Set realistic durations (25-90 min sessions)
                - Include review sessions and buffer time
                - Ask for confirmation before creating tasks
                - Output as structured task list with due dates
                """;
            case "INTERVIEW" -> """
                MODE: INTERVIEW - Conduct mock interviews with feedback.
                - Ask about target role/area (Java, DSA, System Design, etc.)
                - Ask one interview question at a time
                - Evaluate answer: correctness, completeness, clarity
                - Provide constructive feedback and ideal answer
                - Track performance across categories
                - End with summary and improvement plan
                """;
            case "IMPROVE" -> """
                MODE: IMPROVE - Analyze performance and suggest improvements.
                - Review student's completed tasks, goals, lessons
                - Identify patterns: weak topics, missed deadlines, strengths
                - Suggest specific, actionable improvements
                - Prioritize by impact and effort
                - Never fabricate data - only use provided context
                - If insufficient data, recommend diagnostic quiz
                """;
            default -> getModeInstructions("LEARN");
        };
    }

    private String buildFullPrompt(String systemPrompt, List<ChatDtos.MessageResponse> history, String userMessage) {
        StringBuilder sb = new StringBuilder(systemPrompt).append("\n\nCONVERSATION HISTORY:\n");
        if (history != null && !history.isEmpty()) {
            for (var msg : history.stream().filter(m -> m.content() != null && !m.content().isBlank()).toList()) {
                sb.append(msg.role().equals("user") ? "Student: " : "Coach: ")
                  .append(msg.content()).append("\n");
            }
        } else {
            sb.append("(No previous messages)\n");
        }
        sb.append("\nStudent: ").append(userMessage).append("\n\nCoach:");
        return sb.toString();
    }

    private List<String> extractSuggestedQuestions(String reply, String mode) {
        List<String> questions = new java.util.ArrayList<>();
        try {
            int idx = reply.indexOf("SUGGESTED QUESTIONS:");
            if (idx >= 0) {
                String section = reply.substring(idx + "SUGGESTED QUESTIONS:".length());
                int end = section.indexOf("ACTIONS:");
                if (end < 0) end = section.length();
                String[] lines = section.substring(0, end).split("\\n");
                for (String line : lines) {
                    line = line.trim().replaceFirst("^[-•*\\d.\\s]+", "");
                    if (!line.isBlank() && questions.size() < 5) {
                        questions.add(line);
                    }
                }
            }
        } catch (Exception ignored) {}
        if (questions.isEmpty()) {
            questions.addAll(getDefaultSuggestions(mode));
        }
        return questions;
    }

    private List<CoachResponse.CoachAction> extractActions(String reply, String mode, String conversationId) {
        List<CoachResponse.CoachAction> actions = new java.util.ArrayList<>();
        try {
            int idx = reply.indexOf("ACTIONS:");
            if (idx >= 0) {
                String section = reply.substring(idx + "ACTIONS:".length());
                String[] lines = section.split("\\n");
                for (String line : lines) {
                    line = line.trim().replaceFirst("^[-•*\\d.\\s]+", "");
                    if (line.isBlank()) continue;
                    String type = "START_PRACTICE";
                    String label = line;
                    String payload = "";
                    if (line.toLowerCase().contains("task")) { type = "CREATE_TASK"; }
                    else if (line.toLowerCase().contains("goal")) { type = "OPEN_GOAL"; }
                    else if (line.toLowerCase().contains("lesson")) { type = "GENERATE_LESSON"; }
                    else if (line.toLowerCase().contains("quiz")) { type = "TAKE_QUIZ"; }
                    else if (line.toLowerCase().contains("plan")) { type = "CREATE_PLAN"; }
                    actions.add(new CoachResponse.CoachAction(type, label, payload));
                }
            }
        } catch (Exception ignored) {}
        if (actions.isEmpty()) {
            actions.addAll(getDefaultActions(mode));
        }
        return actions;
    }

    private List<String> getDefaultSuggestions(String mode) {
        return switch (mode) {
            case "LEARN" -> List.of("Give me a real-world example", "Explain this more simply", "Show me a code example", "What are the key concepts?", "How does this relate to what I know?");
            case "PRACTICE" -> List.of("Give me another question", "Explain the solution", "Make it harder", "Make it easier", "Show me a similar problem");
            case "QUIZ" -> List.of("Next question", "Explain this answer", "Show my score", "Review wrong answers", "Try a different topic");
            case "PLAN" -> List.of("Create tasks for this week", "Adjust for less time", "Add review sessions", "Set reminders", "Break down further");
            case "INTERVIEW" -> List.of("Next question", "Better answer example", "Why is this asked?", "Related concepts", "Mock interview summary");
            case "IMPROVE" -> List.of("Show my weak topics", "Create revision plan", "Prioritize my tasks", "Set up study schedule", "Track my progress");
            default -> List.of("Tell me more", "Give an example", "What should I do next?");
        };
    }

    private List<CoachResponse.CoachAction> getDefaultActions(String mode) {
        return switch (mode) {
            case "LEARN" -> List.of(new CoachResponse.CoachAction("START_PRACTICE", "Practice this topic", ""), new CoachResponse.CoachAction("GENERATE_LESSON", "Generate full lesson", ""));
            case "PRACTICE" -> List.of(new CoachResponse.CoachAction("TAKE_QUIZ", "Turn into quiz", ""), new CoachResponse.CoachAction("CREATE_TASK", "Add as study task", ""));
            case "QUIZ" -> List.of(new CoachResponse.CoachAction("CREATE_TASK", "Add weak areas as tasks", ""), new CoachResponse.CoachAction("CREATE_PLAN", "Create improvement plan", ""));
            case "PLAN" -> List.of(new CoachResponse.CoachAction("CREATE_TASK", "Create tasks from plan", ""), new CoachResponse.CoachAction("OPEN_GOAL", "View in goal", ""));
            case "INTERVIEW" -> List.of(new CoachResponse.CoachAction("CREATE_TASK", "Add weak areas as tasks", ""), new CoachResponse.CoachAction("CREATE_PLAN", "Create prep plan", ""));
            case "IMPROVE" -> List.of(new CoachResponse.CoachAction("CREATE_TASK", "Add improvement tasks", ""), new CoachResponse.CoachAction("CREATE_PLAN", "Create weekly plan", ""));
            default -> List.of(new CoachResponse.CoachAction("START_PRACTICE", "Start practicing", ""));
        };
    }

    private String deriveTitle(String message) {
        if (message.length() > 60) return message.substring(0, 57) + "...";
        return message;
    }

    private String extractSubject(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("java")) return "Programming";
        if (lower.contains("sql") || lower.contains("database")) return "Databases";
        if (lower.contains("algorithm") || lower.contains("data structure")) return "Algorithms";
        if (lower.contains("math") || lower.contains("algebra") || lower.contains("calculus")) return "Mathematics";
        if (lower.contains("physics") || lower.contains("chemistry") || lower.contains("biology")) return "Science";
        return "General";
    }

    private String extractTopic(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("recursion")) return "Recursion";
        if (lower.contains("inheritance") || lower.contains("polymorphism")) return "OOP Concepts";
        if (lower.contains("hashmap") || lower.contains("hash map")) return "HashMap";
        if (lower.contains("arraylist") || lower.contains("array list")) return "ArrayList";
        if (lower.contains("join") || lower.contains("sql join")) return "SQL Joins";
        if (lower.contains("dynamic programming")) return "Dynamic Programming";
        return message.length() > 40 ? message.substring(0, 40) : message;
    }

    private CoachResponse fallbackResponse(String conversationId, String mode, String userMessage) {
        return new CoachResponse(
                conversationId,
                "I'm having trouble connecting to the AI service right now. " +
                "Please try again in a moment, or let me know if you'd like to continue with a different topic.",
                mode,
                List.of("Try again", "Change topic", "Check connection"),
                List.of(new CoachResponse.CoachAction("START_PRACTICE", "Retry", "")),
                new CoachResponse.CoachMetadata("none", "none", 0, false)
        );
    }
}