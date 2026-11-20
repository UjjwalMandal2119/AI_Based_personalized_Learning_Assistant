import { Component, inject, signal, computed, effect, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LearningService } from '../core/learning.service';
import { AuthService } from '../core/auth.service';
import {
  CoachMode,
  CoachModeInfo,
  CoachRequest,
  CoachResponse,
  CoachMessage,
  CoachConversation,
  CoachAction
} from '../core/models';
import { ErrorMessageComponent } from '../shared/error-message';
import { LoadingStateComponent } from '../shared/loading-state';

@Component({
  selector: 'app-ai-coach-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, ErrorMessageComponent, LoadingStateComponent],
  template: `
    @if (isOpen()) {
      <div class="modal-backdrop" (click)="close()" role="dialog" aria-modal="true" aria-labelledby="coach-title">
        <div class="modal-container" (click)="$event.stopPropagation()">
          <!-- Header -->
          <header class="modal-header">
            <div class="header-left">
              <span class="coach-icon" aria-hidden="true">✦</span>
              <h2 id="coach-title">AI Learning Coach</h2>
            </div>
            <div class="header-right">
              <select
                class="mode-select"
                [(ngModel)]="selectedMode"
                (ngModelChange)="onModeChange($event)"
                [disabled]="isLoading()"
                aria-label="Coach mode">
                @for (mode of modes(); track mode.value) {
                  <option [value]="mode.value">{{ mode.label }}</option>
                }
              </select>
              <button class="icon-button" (click)="newConversation()" [disabled]="isLoading()" aria-label="New conversation">
                <span aria-hidden="true">＋</span>
              </button>
              <button class="icon-button" (click)="close()" [disabled]="isLoading()" aria-label="Close coach">
                <span aria-hidden="true">✕</span>
              </button>
            </div>
          </header>

          <!-- Conversation List (Sidebar on desktop, drawer on mobile) -->
          @if (showConversations()) {
            <aside class="conversations-panel" [class.open]="showConversations()">
              <div class="conversations-header">
                <h3>Conversations</h3>
                <button class="icon-button" (click)="toggleConversations()" aria-label="Close conversations">
                  <span aria-hidden="true">✕</span>
                </button>
              </div>
              <div class="conversations-list">
                @if (conversations().length === 0) {
                  <p class="empty-conversations">No conversations yet. Start by asking a question!</p>
                } @else {
                  @for (conv of conversations(); track conv.id) {
                    <button
                      class="conversation-item"
                      [class.active]="activeConversationId() === conv.id"
                      (click)="loadConversation(conv.id)"
                      [disabled]="isLoading()">
                      <span class="conv-title">{{ conv.title }}</span>
                      <span class="conv-meta">{{ conv.updatedAt | date:'short' }}</span>
                    </button>
                  }
                }
              </div>
            </aside>
          }

          <!-- Main Chat Area -->
          <main class="chat-area" [class.with-sidebar]="showConversations()">
            @if (isLoading() && messages().length === 0) {
              <app-loading-state message="Loading conversation…" />
            } @else if (error()) {
              <app-error-message [message]="error() || 'An error occurred'" />
            } @else {
              <div class="messages-container" #messagesContainer>
                @if (messages().length === 0 && !isLoading()) {
                  <div class="welcome-state">
                    <div class="welcome-icon" aria-hidden="true">✦</div>
                    <h3>Welcome to your AI Learning Coach!</h3>
                    <p class="welcome-text">{{ welcomeMessage() }}</p>
                    <div class="suggested-chips">
                      @for (q of suggestedQuestions(); track $index) {
                        <button class="suggested-chip" (click)="sendSuggested(q)" [disabled]="isLoading()">
                          {{ q }}
                        </button>
                      }
                    </div>
                    <p class="mode-hint">Mode: <strong>{{ currentModeLabel() }}</strong> — {{ currentModeDescription() }}</p>
                  </div>
                } @else {
                  @for (msg of messages(); track msg.id) {
                    <div class="message" [class.user]="msg.role === 'user'" [class.assistant]="msg.role === 'assistant'">
                      <div class="message-avatar" aria-hidden="true">
                        @if (msg.role === 'user') { {{ auth.currentUser()?.displayName?.charAt(0)?.toUpperCase() || 'U' }} }
                        @else { ✦ }
                      </div>
                      <div class="message-content">
                        <div class="message-text" [innerHTML]="formatMessage(msg.content)"></div>
                        @if (msg.role === 'assistant' && msg.providerName) {
                          <div class="message-meta">
                            <span class="provider-badge" [class.fallback]="msg.providerName === 'gemini'">
                              {{ msg.providerName }} ({{ msg.modelName }})
                            </span>
                          </div>
                        }
                      </div>
                    </div>
                  }
                  @if (isLoading()) {
                    <div class="message assistant typing">
                      <div class="message-avatar" aria-hidden="true">✦</div>
                      <div class="message-content">
                        <div class="typing-indicator">
                          <span></span><span></span><span></span>
                        </div>
                      </div>
                    </div>
                  }
                }
              </div>

              <!-- Suggested Follow-ups (after assistant message) -->
              @if (lastAssistantResponse()?.suggestedQuestions?.length) {
                <div class="followup-chips">
                  @for (q of lastAssistantResponse()!.suggestedQuestions; track $index) {
                    <button class="followup-chip" (click)="sendSuggested(q)" [disabled]="isLoading()">
                      {{ q }}
                    </button>
                  }
                </div>
              }

              <!-- Action Buttons (after assistant message) -->
              @if (lastAssistantResponse()?.actions?.length) {
                <div class="action-buttons">
                  @for (action of lastAssistantResponse()!.actions; track $index) {
                    <button class="action-button" (click)="handleAction(action)" [disabled]="isLoading()">
                      {{ action.label }}
                    </button>
                  }
                </div>
              }
            }
          </main>

          <!-- Input Area -->
          <footer class="modal-footer">
            <div class="input-wrapper">
              <textarea
                class="message-input"
                [(ngModel)]="inputMessage"
                (keydown.enter)="onEnter($event)"
                (input)="autoResize($event)"
                placeholder="Ask me anything about your learning…"
                [disabled]="isLoading()"
                rows="1"
                aria-label="Your message"></textarea>
              <button
                class="send-button"
                (click)="sendMessage()"
                [disabled]="isLoading() || !inputMessage.trim()"
                aria-label="Send message">
                <span aria-hidden="true">{{ isLoading() ? '⏳' : '➤' }}</span>
              </button>
            </div>
            <p class="input-hint">Press Enter to send, Shift+Enter for new line</p>
          </footer>
        </div>
      </div>
    }
  `,
  styles: [`
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 1rem;
      animation: fadeIn 0.2s ease;
    }

    .modal-container {
      width: 100%;
      max-width: 900px;
      max-height: 90vh;
      background: var(--surface, #fff);
      border-radius: 16px;
      box-shadow: 0 24px 48px rgba(0, 0, 0, 0.15);
      display: flex;
      flex-direction: column;
      overflow: hidden;
      animation: slideUp 0.3s ease;
    }

    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
    @keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: none; } }

    .modal-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1rem 1.25rem;
      border-bottom: 1px solid var(--border, #e5e5e5);
      background: var(--surface-alt, #fafafa);
    }

    .header-left { display: flex; align-items: center; gap: 0.5rem; }
    .coach-icon { font-size: 1.5rem; }
    .modal-header h2 { margin: 0; font-size: 1.1rem; font-weight: 600; color: var(--ink, #1a1a1a); }

    .header-right { display: flex; align-items: center; gap: 0.5rem; }
    .mode-select {
      padding: 0.375rem 2rem 0.375rem 0.75rem;
      border: 1px solid var(--border, #ddd);
      border-radius: 8px;
      background: var(--surface, #fff);
      font-size: 0.85rem;
      cursor: pointer;
    }
    .mode-select:disabled { opacity: 0.6; cursor: not-allowed; }

    .icon-button {
      width: 36px; height: 36px;
      display: grid; place-items: center;
      border: 1px solid var(--border, #ddd);
      border-radius: 10px;
      background: var(--surface, #fff);
      cursor: pointer;
      font-size: 1rem;
      transition: all 0.15s;
    }
    .icon-button:hover:not(:disabled) { background: var(--surface-alt, #f5f5f5); }
    .icon-button:disabled { opacity: 0.5; cursor: not-allowed; }

    .conversations-panel {
      position: absolute;
      top: 0; left: 0; bottom: 0;
      width: 280px;
      background: var(--surface-alt, #fafafa);
      border-right: 1px solid var(--border, #e5e5e5);
      display: flex;
      flex-direction: column;
      transform: translateX(-100%);
      transition: transform 0.25s ease;
      z-index: 10;
    }
    .conversations-panel.open { transform: translateX(0); }

    .conversations-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 0.75rem 1rem;
      border-bottom: 1px solid var(--border, #e5e5e5);
    }
    .conversations-header h3 { margin: 0; font-size: 0.9rem; font-weight: 600; }

    .conversations-list { flex: 1; overflow-y: auto; padding: 0.5rem; }
    .empty-conversations { font-size: 0.85rem; color: var(--muted, #888); text-align: center; padding: 2rem; }

    .conversation-item {
      width: 100%; text-align: left; padding: 0.75rem;
      border: none; background: transparent; border-radius: 8px;
      cursor: pointer; transition: background 0.15s;
      display: flex; flex-direction: column; gap: 0.25rem;
    }
    .conversation-item:hover:not(:disabled) { background: var(--surface, #fff); }
    .conversation-item.active { background: var(--primary-light, #eef5ff); }
    .conversation-item:disabled { opacity: 0.6; cursor: not-allowed; }
    .conv-title { font-size: 0.85rem; font-weight: 500; color: var(--ink, #1a1a1a); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .conv-meta { font-size: 0.7rem; color: var(--muted, #888); }

    .chat-area {
      flex: 1; min-height: 0; display: flex; flex-direction: column;
      margin-left: 0;
      transition: margin-left 0.25s ease;
    }
    .chat-area.with-sidebar { margin-left: 280px; }

    .messages-container {
      flex: 1; overflow-y: auto; padding: 1.25rem;
      display: flex; flex-direction: column; gap: 1rem;
    }

    .welcome-state {
      text-align: center; padding: 2rem 1rem; max-width: 480px; margin: 0 auto;
    }
    .welcome-icon { font-size: 3rem; margin-bottom: 0.75rem; }
    .welcome-state h3 { margin: 0 0 0.5rem; font-size: 1.25rem; color: var(--ink, #1a1a1a); }
    .welcome-text { color: var(--muted, #666); margin: 0 0 1.5rem; line-height: 1.6; }

    .suggested-chips { display: flex; flex-wrap: wrap; gap: 0.5rem; justify-content: center; margin-bottom: 1rem; }
    .suggested-chip {
      padding: 0.5rem 1rem; border: 1px solid var(--border, #ddd);
      border-radius: 20px; background: var(--surface, #fff);
      font-size: 0.8rem; cursor: pointer; transition: all 0.15s;
    }
    .suggested-chip:hover:not(:disabled) { border-color: var(--primary, #3b82f6); color: var(--primary, #3b82f6); }
    .suggested-chip:disabled { opacity: 0.6; cursor: not-allowed; }

    .mode-hint { font-size: 0.8rem; color: var(--muted, #888); margin-top: 1rem; }
    .mode-hint strong { color: var(--ink, #1a1a1a); }

    .message { display: flex; gap: 0.75rem; max-width: 85%; animation: messageIn 0.2s ease; }
    @keyframes messageIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: none; } }
    .message.user { align-self: flex-end; flex-direction: row-reverse; }
    .message.assistant { align-self: flex-start; }

    .message-avatar {
      width: 32px; height: 32px; border-radius: 50%;
      display: grid; place-items: center; flex-shrink: 0;
      font-size: 0.85rem; font-weight: 600;
    }
    .message.user .message-avatar { background: var(--primary, #3b82f6); color: white; }
    .message.assistant .message-avatar { background: var(--accent, #f59e0b); color: white; }

    .message-content { flex: 1; min-width: 0; }
    .message-text { line-height: 1.6; font-size: 0.9rem; color: var(--ink, #1a1a1a); white-space: pre-wrap; word-wrap: break-word; }
    .message-text h1, .message-text h2, .message-text h3 { margin: 0.75rem 0 0.5rem; font-weight: 600; }
    .message-text h1 { font-size: 1.2rem; }
    .message-text h2 { font-size: 1.1rem; }
    .message-text h3 { font-size: 1rem; }
    .message-text p { margin: 0.5rem 0; }
    .message-text ul, .message-text ol { margin: 0.5rem 0; padding-left: 1.5rem; }
    .message-text li { margin: 0.25rem 0; }
    .message-text code {
      background: var(--surface-alt, #f0f0f0); padding: 0.125rem 0.375rem;
      border-radius: 4px; font-family: monospace; font-size: 0.85em;
    }
    .message-text pre {
      background: #1e1e1e; color: #d4d4d4; padding: 1rem; border-radius: 8px;
      overflow-x: auto; margin: 0.75rem 0; font-size: 0.8rem; line-height: 1.5;
    }
    .message-text pre code { background: transparent; padding: 0; color: inherit; }
    .message-text strong { font-weight: 600; }
    .message-text blockquote { border-left: 3px solid var(--primary, #3b82f6); padding-left: 1rem; margin: 0.75rem 0; color: var(--muted, #666); font-style: italic; }

    .message-meta { margin-top: 0.5rem; display: flex; gap: 0.5rem; align-items: center; }
    .provider-badge {
      font-size: 0.65rem; padding: 0.125rem 0.5rem; border-radius: 10px;
      background: var(--success-light, #e8f4e8); color: var(--success, #2d6a2d);
      font-family: monospace;
    }
    .provider-badge.fallback { background: var(--warning-light, #fdf3e0); color: var(--warning, #8a5a00); }

    .typing-indicator { display: flex; gap: 4px; padding: 0.5rem 0; }
    .typing-indicator span {
      width: 8px; height: 8px; border-radius: 50%;
      background: var(--muted, #888); animation: bounce 1.4s infinite ease-in-out both;
    }
    .typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
    .typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
    @keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }

    .followup-chips, .action-buttons {
      display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 0 1.25rem 0.75rem;
      border-top: 1px solid var(--border, #eee); margin-top: -0.5rem;
    }
    .followup-chip {
      padding: 0.375rem 0.875rem; border: 1px solid var(--border, #ddd);
      border-radius: 16px; background: var(--surface, #fff);
      font-size: 0.75rem; cursor: pointer; transition: all 0.15s;
    }
    .followup-chip:hover:not(:disabled) { border-color: var(--primary, #3b82f6); color: var(--primary, #3b82f6); }
    .followup-chip:disabled { opacity: 0.6; cursor: not-allowed; }

    .action-button {
      padding: 0.5rem 1rem; border: 1px solid var(--primary, #3b82f6);
      border-radius: 8px; background: var(--primary, #3b82f6); color: white;
      font-size: 0.8rem; font-weight: 500; cursor: pointer; transition: all 0.15s;
    }
    .action-button:hover:not(:disabled) { background: var(--primary-dark, #2563eb); border-color: var(--primary-dark, #2563eb); }
    .action-button:disabled { opacity: 0.6; cursor: not-allowed; }

    .modal-footer {
      border-top: 1px solid var(--border, #e5e5e5);
      padding: 0.75rem 1rem; background: var(--surface-alt, #fafafa);
    }
    .input-wrapper { display: flex; gap: 0.5rem; align-items: flex-end; }
    .message-input {
      flex: 1; min-height: 44px; max-height: 160px;
      padding: 0.625rem 0.875rem; border: 1px solid var(--border, #ddd);
      border-radius: 12px; background: var(--surface, #fff);
      font: inherit; font-size: 0.9rem; line-height: 1.5;
      resize: none; outline: none; transition: border-color 0.15s, box-shadow 0.15s;
    }
    .message-input:focus { border-color: var(--primary, #3b82f6); box-shadow: 0 0 0 3px var(--primary-light, #eef5ff); }
    .message-input:disabled { background: var(--surface-alt, #f5f5f5); }

    .send-button {
      width: 44px; height: 44px; border-radius: 12px;
      background: var(--primary, #3b82f6); color: white;
      border: none; cursor: pointer; display: grid; place-items: center;
      font-size: 1.25rem; transition: background 0.15s;
    }
    .send-button:hover:not(:disabled) { background: var(--primary-dark, #2563eb); }
    .send-button:disabled { opacity: 0.5; cursor: not-allowed; }

    .input-hint { margin: 0.5rem 0 0; font-size: 0.7rem; color: var(--muted, #888); text-align: center; }

    @media (max-width: 768px) {
      .modal-container { max-height: 100vh; border-radius: 0; max-width: none; }
      .conversations-panel { position: fixed; top: 0; left: 0; bottom: 0; z-index: 1001; width: 85vw; max-width: 320px; }
      .chat-area.with-sidebar { margin-left: 0; }
      .message { max-width: 90%; }
      .modal-footer { padding: 0.75rem; position: sticky; bottom: 0; }
    }

    @media (prefers-reduced-motion: reduce) {
      *, *::before, *::after { animation-duration: 0.01ms !important; transition-duration: 0.01ms !important; }
    }
  `]
})
export class AiCoachModalComponent implements OnInit, OnDestroy {
  private readonly learning = inject(LearningService);
  readonly auth = inject(AuthService);

  isOpen = signal(false);
  selectedMode = signal<CoachMode>('LEARN');
  messages = signal<CoachMessage[]>([]);
  conversations = signal<CoachConversation[]>([]);
  activeConversationId = signal<string | null>(null);
  isLoading = signal(false);
  error = signal<string | null>(null);
  inputMessage = '';
  showConversations = signal(false);
  modes = signal<CoachModeInfo[]>([]);
  lastResponse = signal<CoachResponse | null>(null);

  readonly lastAssistantResponse = computed(() => {
    const resp = this.lastResponse();
    if (!resp) return null;
    // Return the last assistant-like response from the response
    return {
      suggestedQuestions: resp.suggestedQuestions,
      actions: resp.actions
    };
  });

  readonly currentModeLabel = computed(() => {
    const mode = this.modes().find(m => m.value === this.selectedMode());
    return mode?.label || this.selectedMode();
  });

  readonly currentModeDescription = computed(() => {
    const mode = this.modes().find(m => m.value === this.selectedMode());
    return mode?.description || '';
  });

  readonly welcomeMessage = computed(() => {
    const user = this.auth.currentUser();
    const name = user?.displayName || 'there';
    const hour = new Date().getHours();
    const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
    return `${greeting}, ${name}! I'm your AI Learning Coach. I can help you understand concepts, practice, take quizzes, plan your studies, prepare for interviews, and improve your performance. What would you like to work on today?`;
  });

  readonly suggestedQuestions = computed(() => {
    const mode = this.selectedMode();
    switch (mode) {
      case 'LEARN': return ['Explain a difficult topic', 'Teach me step by step', 'Give me a real-world example', 'Compare two concepts'];
      case 'PRACTICE': return ['Generate practice questions', 'Interactive quiz', 'Test my understanding', 'Explain why my answer is wrong'];
      case 'QUIZ': return ['Start a quiz', 'Quiz me on my weak topics', 'Interview questions', 'Timed practice'];
      case 'PLAN': return ['Create study plan for today', '7-day schedule', 'Help complete pending tasks', 'Achieve my learning goal'];
      case 'INTERVIEW': return ['Java & OOP interview', 'Data Structures & Algorithms', 'System Design', 'Full-stack questions'];
      case 'IMPROVE': return ['Analyze my progress', 'Identify weak topics', 'Suggest improvements', 'Revision plan'];
      default: return ['Help me learn', 'Create a plan', 'Practice questions'];
    }
  });

  ngOnInit() {
    this.learning.coachModes().subscribe({
      next: (modes) => this.modes.set(modes),
      error: () => this.modes.set([
        { value: 'LEARN', label: 'Learn', description: 'Explain concepts step by step with examples' },
        { value: 'PRACTICE', label: 'Practice', description: 'Generate questions and evaluate your answers' },
        { value: 'QUIZ', label: 'Quiz', description: 'Interactive quizzes with scoring and feedback' },
        { value: 'PLAN', label: 'Plan', description: 'Create study schedules and learning plans' },
        { value: 'INTERVIEW', label: 'Interview', description: 'Mock interviews with detailed feedback' },
        { value: 'IMPROVE', label: 'Improve', description: 'Analyze performance and suggest improvements' }
      ])
    });
  }

  ngOnDestroy() {}

  open(conversationId?: string) {
    this.isOpen.set(true);
    this.error.set(null);
    this.loadConversations();
    if (conversationId) {
      this.loadConversation(conversationId);
    } else if (this.activeConversationId()) {
      this.loadConversation(this.activeConversationId()!);
    }
    document.body.style.overflow = 'hidden';
  }

  close() {
    this.isOpen.set(false);
    this.showConversations.set(false);
    document.body.style.overflow = '';
  }

  toggleConversations() {
    this.showConversations.update(v => !v);
  }

  loadConversations() {
    this.learning.coachConversations().subscribe({
      next: (convs) => this.conversations.set(convs),
      error: () => {} // Silent fail
    });
  }

  loadConversation(id: string) {
    this.isLoading.set(true);
    this.error.set(null);
    this.learning.coachConversation(id).subscribe({
      next: (detail) => {
        this.messages.set(detail.messages || []);
        this.activeConversationId.set(detail.id);
        this.showConversations.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.detail || 'Could not load conversation');
      },
      complete: () => this.isLoading.set(false)
    });
  }

  newConversation() {
    this.messages.set([]);
    this.activeConversationId.set(null);
    this.showConversations.set(false);
    this.error.set(null);
  }

  onModeChange(mode: CoachMode) {
    this.selectedMode.set(mode);
    this.newConversation();
  }

  onEnter(event: Event) {
    const kbEvent = event as KeyboardEvent;
    if (!kbEvent.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  autoResize(event: Event) {
    const textarea = event.target as HTMLTextAreaElement;
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 160) + 'px';
  }

  sendMessage() {
    const content = this.inputMessage.trim();
    if (!content || this.isLoading()) return;

    const userMessage: CoachMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content,
      providerName: null,
      modelName: null,
      createdAt: new Date().toISOString()
    };
    this.messages.update(m => [...m, userMessage]);
    this.inputMessage = '';
    const textarea = document.querySelector('.message-input') as HTMLTextAreaElement;
    if (textarea) textarea.style.height = 'auto';

    this.sendToCoach(content, userMessage.id);
  }

  sendSuggested(question: string) {
    const userMessage: CoachMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content: question,
      providerName: null,
      modelName: null,
      createdAt: new Date().toISOString()
    };
    this.messages.update(m => [...m, userMessage]);
    this.sendToCoach(question, userMessage.id);
  }

  private sendToCoach(message: string, tempUserMsgId: string) {
    this.isLoading.set(true);
    this.error.set(null);

    const request: CoachRequest = {
      conversationId: this.activeConversationId(),
      message,
      mode: this.selectedMode(),
      history: this.messages().filter(m => m.id !== tempUserMsgId)
    };

    this.learning.coachRespond(request).subscribe({
      next: (response) => {
        this.lastResponse.set(response);
        const assistantMessage: CoachMessage = {
          id: crypto.randomUUID(),
          role: 'assistant',
          content: response.reply,
          providerName: response.metadata.provider,
          modelName: response.metadata.model,
          createdAt: new Date().toISOString()
        };
        this.messages.update(m => [...m, assistantMessage]);
        this.activeConversationId.set(response.conversationId);
        this.scrollToBottom();
      },
      error: (err) => {
        this.error.set(err.error?.detail || 'Failed to get response. Please try again.');
        this.messages.update(m => m.filter(m => m.id !== tempUserMsgId));
      },
      complete: () => this.isLoading.set(false)
    });
  }

  handleAction(action: CoachAction) {
    // Handle action buttons from AI response
    switch (action.type) {
      case 'START_PRACTICE':
        this.selectedMode.set('PRACTICE');
        this.newConversation();
        this.sendSuggested('Give me a practice question on this topic');
        break;
      case 'TAKE_QUIZ':
        this.selectedMode.set('QUIZ');
        this.newConversation();
        this.sendSuggested('Start a quiz on this topic');
        break;
      case 'CREATE_TASK':
        // Could navigate to goal detail with pre-filled task
        this.close();
        break;
      case 'CREATE_PLAN':
        this.selectedMode.set('PLAN');
        this.newConversation();
        this.sendSuggested('Create a study plan for this');
        break;
      case 'GENERATE_LESSON':
        // Could trigger lesson generation
        this.close();
        break;
      case 'OPEN_GOAL':
        this.close();
        break;
      default:
        this.sendSuggested(action.label);
    }
  }

  formatMessage(content: string): string {
    // Simple markdown-like formatting
    return content
      .replace(/^### (.*$)/gm, '<h3>$1</h3>')
      .replace(/^## (.*$)/gm, '<h2>$1</h2>')
      .replace(/^# (.*$)/gm, '<h1>$1</h1>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code class="language-$1">$2</code></pre>')
      .replace(/^\- (.*$)/gm, '<li>$1</li>')
      .replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
      .replace(/\n/g, '<br>');
  }

  private scrollToBottom() {
    setTimeout(() => {
      const container = document.querySelector('.messages-container');
      if (container) container.scrollTop = container.scrollHeight;
    }, 50);
  }
}