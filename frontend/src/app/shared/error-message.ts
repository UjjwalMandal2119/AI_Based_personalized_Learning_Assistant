import { Component, input } from '@angular/core';

@Component({
  selector: 'app-error-message',
  standalone: true,
  template: `<p class="error-message" role="alert">{{ message() }}</p>`,
})
export class ErrorMessageComponent {
  readonly message = input.required<string>();
}
