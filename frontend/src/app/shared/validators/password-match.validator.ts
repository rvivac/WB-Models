import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Validador customizado para confirmação de senha idêntica em tempo real.
 * Compara os campos 'password' e 'confirmPassword' no FormGroup.
 */
export const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');

  if (!password || !confirmPassword) {
    return null;
  }

  // Só aciona erro de mismatch se o campo de confirmação tiver valor preenchido ou for tocado
  if (!confirmPassword.value) {
    return null;
  }

  return password.value === confirmPassword.value ? null : { mismatch: true };
};

/**
 * Validador de política de senha forte:
 * - Mínimo de 8 caracteres
 * - Pelo menos 1 letra maiúscula
 * - Pelo menos 1 letra minúscula
 * - Pelo menos 1 número
 * - Pelo menos 1 caractere especial (!@#$%^&*...)
 */
export const strongPasswordValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (!value) {
    return null;
  }

  const hasMinLength = value.length >= 8;
  const hasUpper = /[A-Z]/.test(value);
  const hasLower = /[a-z]/.test(value);
  const hasNumber = /[0-9]/.test(value);
  const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?`~]/.test(value);

  const isValid = hasMinLength && hasUpper && hasLower && hasNumber && hasSpecial;

  return isValid
    ? null
    : {
        strongPassword: {
          hasMinLength,
          hasUpper,
          hasLower,
          hasNumber,
          hasSpecial
        }
      };
};
