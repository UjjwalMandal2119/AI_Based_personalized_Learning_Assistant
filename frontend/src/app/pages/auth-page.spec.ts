import { FormControl, FormGroup, Validators } from '@angular/forms';
import { passwordsMatchValidator } from './auth-page';

describe('passwordsMatchValidator', () => {
  const form = new FormGroup({
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    confirmPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  }, { validators: passwordsMatchValidator });

  it('accepts matching passwords', () => {
    form.setValue({ password: 'study-1234', confirmPassword: 'study-1234' });

    expect(form.hasError('passwordMismatch')).toBe(false);
  });

  it('rejects passwords that do not match', () => {
    form.setValue({ password: 'study-1234', confirmPassword: 'other-1234' });

    expect(form.hasError('passwordMismatch')).toBe(true);
  });
});
