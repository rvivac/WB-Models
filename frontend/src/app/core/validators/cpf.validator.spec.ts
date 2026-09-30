import { FormControl } from '@angular/forms';
import { cpfValidator } from './cpf.validator';

describe('cpfValidator', () => {
  const validator = cpfValidator();

  it('should return null for empty or null values', () => {
    expect(validator(new FormControl(''))).toBeNull();
    expect(validator(new FormControl(null))).toBeNull();
    expect(validator(new FormControl(undefined))).toBeNull();
  });

  it('should return invalidCpf for invalid length or non-digits', () => {
    expect(validator(new FormControl('123'))).toEqual({ invalidCpf: true });
    expect(validator(new FormControl('1234567890'))).toEqual({ invalidCpf: true });
    expect(validator(new FormControl('123456789012'))).toEqual({ invalidCpf: true });
  });

  it('should return invalidCpf for sequences of same digits', () => {
    expect(validator(new FormControl('111.111.111-11'))).toEqual({ invalidCpf: true });
    expect(validator(new FormControl('00000000000'))).toEqual({ invalidCpf: true });
    expect(validator(new FormControl('99999999999'))).toEqual({ invalidCpf: true });
  });

  it('should return invalidCpf for invalid verification digits', () => {
    expect(validator(new FormControl('123.456.789-00'))).toEqual({ invalidCpf: true });
    expect(validator(new FormControl('52998224724'))).toEqual({ invalidCpf: true });
  });

  it('should return null for valid CPFs', () => {
    // CPFs válidos conhecidos para teste algorítmico:
    // 52998224725 -> 5+2+9+9+8+2+2+4+7
    expect(validator(new FormControl('52998224725'))).toBeNull();
    expect(validator(new FormControl('529.982.247-25'))).toBeNull();
  });
});
