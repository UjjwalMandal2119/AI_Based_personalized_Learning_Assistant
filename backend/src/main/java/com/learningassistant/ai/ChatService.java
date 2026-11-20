package com.learningassistant.ai;

import com.learningassistant.chat.ChatMessage;
import com.learningassistant.chat.Conversation;
import com.learningassistant.chat.ConversationRepository;
import com.learningassistant.chat.ChatMessageRepository;
import com.learningassistant.chat.ChatDtos;
import com.learningassistant.student.Student;
import com.learningassistant.student.StudentRepository;
import com.learningassistant.common.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates conversation persistence for the AI coach.
 *
 * <p>Every operation is scoped to the authenticated student. Students can only
 * access their own conversations and messages.</p>
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final StudentRepository students;

    public ChatService(ConversationRepository conversations,
                       ChatMessageRepository messages,
                       StudentRepository students) {
        this.conversations = conversations;
        this.messages = messages;
        this.students = students;
    }

    @Transactional(readOnly = true)
    public List<ChatDtos.ConversationSummary> list(String email) {
        return conversations.findAllByStudent_EmailOrderByUpdatedAtDesc(email)
                .stream().map(this::toSummary).toList();
    }

    @Transactional
    public ChatDtos.ConversationDetail create(String email, ChatDtos.CreateConversationRequest request) {
        Student student = students.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student"));
        Conversation conv = conversations.save(new Conversation(student, request.title(), request.subject(), request.topic()));
        return new ChatDtos.ConversationDetail(conv.getId(), conv.getTitle(), conv.getSubject(),
                conv.getTopic(), conv.getCreatedAt(), conv.getUpdatedAt(), List.of());
    }

    @Transactional(readOnly = true)
    public ChatDtos.ConversationDetail get(String email, UUID conversationId) {
        Conversation conv = conversations.findByIdAndStudent_Email(conversationId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation"));
        List<ChatDtos.MessageResponse> msgs = messages.findAllByConversation_IdOrderByCreatedAtAsc(conv.getId())
                .stream().map(ChatDtos.MessageResponse::from).toList();
        return new ChatDtos.ConversationDetail(conv.getId(), conv.getTitle(), conv.getSubject(),
                conv.getTopic(), conv.getCreatedAt(), conv.getUpdatedAt(), msgs);
    }

    @Transactional
    public ChatDtos.ConversationDetail rename(String email, UUID conversationId, ChatDtos.RenameConversationRequest request) {
        Conversation conv = conversations.findByIdAndStudent_Email(conversationId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation"));
        conv.rename(request.title());
        List<ChatDtos.MessageResponse> msgs = messages.findAllByConversation_IdOrderByCreatedAtAsc(conv.getId())
                .stream().map(ChatDtos.MessageResponse::from).toList();
        return new ChatDtos.ConversationDetail(conv.getId(), conv.getTitle(), conv.getSubject(),
                conv.getTopic(), conv.getCreatedAt(), conv.getUpdatedAt(), msgs);
    }

    @Transactional
    public ChatDtos.MessageResponse sendMessage(String email, UUID conversationId, ChatDtos.SendMessageRequest request) {
        Conversation conv = conversations.findByIdAndStudent_Email(conversationId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation"));
        ChatMessage userMsg = messages.save(new ChatMessage(conv, "user", request.content(), null, null));
        return ChatDtos.MessageResponse.from(userMsg);
    }

    @Transactional
    public ChatDtos.MessageResponse appendAssistantMessage(String email, UUID conversationId,
                                                           String content, String provider, String model) {
        Conversation conv = conversations.findByIdAndStudent_Email(conversationId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation"));
        ChatMessage assistantMsg = messages.save(new ChatMessage(conv, "assistant", content, provider, model));
        return ChatDtos.MessageResponse.from(assistantMsg);
    }

    @Transactional
    public ChatDtos.ConversationDetail ensureConversation(String email, String title, String subject, String topic) {
        return students.findByEmail(email)
                .map(student -> {
                    Conversation conv = conversations.save(new Conversation(student, title, subject, topic));
                    return new ChatDtos.ConversationDetail(conv.getId(), conv.getTitle(), conv.getSubject(),
                            conv.getTopic(), conv.getCreatedAt(), conv.getUpdatedAt(), List.of());
                })
                .orElseThrow(() -> new ResourceNotFoundException("Student"));
    }

    private ChatDtos.ConversationSummary toSummary(Conversation c) {
        int count = (int) messages.findAllByConversation_IdOrderByCreatedAtAsc(c.getId()).size();
        return new ChatDtos.ConversationSummary(c.getId(), c.getTitle(), c.getSubject(),
                c.getTopic(), c.getCreatedAt(), c.getUpdatedAt(), count);
    }

    Student getStudent(String email) {
        return students.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student"));
    }
}