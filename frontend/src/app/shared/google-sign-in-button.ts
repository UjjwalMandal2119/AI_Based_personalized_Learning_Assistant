import { AfterViewInit, Component, ElementRef, input, output, signal, viewChild } from '@angular/core';
import { environment } from '../../environments/environment';

type GoogleButtonText = 'signin_with' | 'continue_with';

interface GoogleCredentialResponse {
  credential: string;
}

interface GoogleIdentityServices {
  accounts: {
    id: {
      initialize(options: {
        client_id: string;
        callback: (response: GoogleCredentialResponse) => void;
      }): void;
      renderButton(
        parent: HTMLElement,
        options: {
          theme: 'outline';
          size: 'large';
          text: GoogleButtonText;
          shape: 'rectangular';
          width: number;
        },
      ): void;
    };
  };
}

declare global {
  interface Window {
    google?: GoogleIdentityServices;
  }
}

let googleScriptLoad: Promise<void> | null = null;

@Component({
  selector: 'app-google-sign-in-button',
  standalone: true,
  template: `
    @if (clientId()) {
      <div #buttonHost class="google-button-host"></div>
      @if (loadError()) { <p class="field-error" role="alert">{{ loadError() }}</p> }
    } @else {
      <p class="muted" role="status">Google sign-in is not configured yet.</p>
    }
  `,
  styles: [':host { display: block; min-height: 40px; } .google-button-host { display: flex; justify-content: center; }'],
})
export class GoogleSignInButtonComponent implements AfterViewInit {
  readonly text = input<GoogleButtonText>('continue_with');
  readonly clientId = input(environment.googleClientId);
  readonly credential = output<string>();
  readonly loadError = signal('');
  private readonly buttonHost = viewChild<ElementRef<HTMLDivElement>>('buttonHost');

  ngAfterViewInit(): void {
    if (this.clientId()) void this.initializeButton();
  }

  private async initializeButton(): Promise<void> {
    try {
      await this.loadGoogleIdentityServices();
      const google = window.google;
      const host = this.buttonHost()?.nativeElement;
      if (!google || !host) {
        this.loadError.set('Google sign-in could not be loaded. Please try again later.');
        return;
      }

      google.accounts.id.initialize({
        client_id: this.clientId(),
        callback: (response) => this.credential.emit(response.credential),
      });
      google.accounts.id.renderButton(host, {
        theme: 'outline',
        size: 'large',
        text: this.text(),
        shape: 'rectangular',
        width: Math.min(400, Math.max(220, Math.floor(host.getBoundingClientRect().width))),
      });
    } catch {
      this.loadError.set('Google sign-in could not be loaded. Please try again later.');
    }
  }

  private loadGoogleIdentityServices(): Promise<void> {
    if (window.google) return Promise.resolve();
    if (!googleScriptLoad) {
      googleScriptLoad = new Promise<void>((resolve, reject) => {
        const script = document.createElement('script');
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.defer = true;
        script.onload = () => resolve();
        script.onerror = () => reject(new Error('Google Identity Services script failed to load'));
        document.head.appendChild(script);
      });
    }
    return googleScriptLoad;
  }
}
