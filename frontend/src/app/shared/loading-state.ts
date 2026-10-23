import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loading-state',
  standalone: true,
  template: `<div class="loading" role="status">{{ message() }}</div>`,
})
export class LoadingStateComponent {
  readonly message = input('Loading your learning space…');
}
