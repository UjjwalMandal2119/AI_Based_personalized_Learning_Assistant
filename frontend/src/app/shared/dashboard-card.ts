import { Component, input } from '@angular/core';

@Component({
  selector: 'app-dashboard-card',
  standalone: true,
  template: `
    <section class="dashboard-card" [attr.aria-label]="title()">
      <div class="card-heading"><h2>{{ title() }}</h2><ng-content select="[card-action]" /></div>
      <ng-content />
    </section>
  `,
})
export class DashboardCardComponent {
  readonly title = input.required<string>();
}
