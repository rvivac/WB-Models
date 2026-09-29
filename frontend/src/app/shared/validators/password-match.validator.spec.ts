import { FormControl, FormGroup } from '@angular/forms';
import {
  passwordMatchValidator,
  strongPasswordValidator
} from './password-match.validator';

describe('Password Validators', () => {
  describe('passwordMatchValidator', () => {
    it('should return null when confirmPassword is empty', () => {
      const form = new FormGroup(
        {
          password: new FormControl('Secret123!'),
          confirmPassword: new FormControl('')
        },
        { validators: passwordMatchValidator }
      );

      expect(form.errors).toBeNull();
    });

    it('should return null when password and confirmPassword match', () => {
      const form = new FormGroup(
        {
          password: new FormControl('Secret123!'),
          confirmPassword: new FormControl('Secret123!')
        },
        { validators: passwordMatchValidator }
      );

      expect(form.errors).toBeNull();
    });

    it('should return mismatch: true when password and confirmPassword differ', () => {
      const form = new FormGroup(
        {
          password: new FormControl('Secret123!'),
          confirmPassword: new FormControl('DifferentPassword1!')
        },
        { validators: passwordMatchValidator }
      );

      expect(form.errors).toEqual({ mismatch: true });
    });
  });

  describe('strongPasswordValidator', () => {
    it('should return null for valid strong password', () => {
      const control = new FormControl('Pass1234!');
      expect(strongPasswordValidator(control)).toBeNull();
    });

    it('should return error when password has less than 8 characters', () => {
      const control = new FormControl('P1!a');
      const error = strongPasswordValidator(control);
      expect(error).not.toBeNull();
      expect(error?.['strongPassword'].hasMinLength).toBeFalse();
    });

    it('should return error when password lacks special character', () => {
      const control = new FormControl('Password1234');
      const error = strongPasswordValidator(control);
      expect(error).not.toBeNull();
      expect(error?.['strongPassword'].hasSpecial).toBeFalse();
    });

    it('should return error when password lacks uppercase letter', () => {
      const control = new FormControl('password1234!');
      const error = strongPasswordValidator(control);
      expect(error).not.toBeNull();
      expect(error?.['strongPassword'].hasUpper).toBeFalse();
    });

    it('should return error when password lacks numbers', () => {
      const control = new FormControl('PasswordSecret!');
      const error = strongPasswordValidator(control);
      expect(error).not.toBeNull();
      expect(error?.['strongPassword'].hasNumber).toBeFalse();
    });
  });
});
