import { Component, input, output } from '@angular/core';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { StudyTask } from '../core/models';

@Component({
  selector: 'app-task-item',
  standalone: true,
  imports: [DatePipe, LowerCasePipe],
  template: `
    <article class="task-item" [class.is-complete]="task().completed">
      <label class="task-check">
        <input type="checkbox" [checked]="task().completed"
               [attr.aria-label]="'Mark ' + task().title + (task().completed ? ' incomplete' : ' complete')"
               (change)="onToggle($event)">
        <span class="checkmark" aria-hidden="true"></span>
      </label>
      <div class="task-copy">
        <h3>{{ task().title }}</h3>
        @if (task().description) { <p>{{ task().description }}</p> }
        <div class="task-meta">
          <span>{{ task().taskType | lowercase }}</span>
          <span>{{ task().estimatedMinutes }} min</span>
          @if (task().dueDate) { <span>Due {{ task().dueDate | date:'mediumDate' }}</span> }
        </div>
      </div>
    </article>
  `,
})
export class TaskItemComponent {
  readonly task = input.required<StudyTask>();
  readonly completionChange = output<{ id: string; completed: boolean }>();

  onToggle(event: Event): void {
    this.completionChange.emit({
      id: this.task().id,
      completed: (event.target as HTMLInputElement).checked,
    });
  }
}
