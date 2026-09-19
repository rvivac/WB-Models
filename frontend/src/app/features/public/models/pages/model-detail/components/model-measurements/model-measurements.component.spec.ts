import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ModelMeasurementsComponent } from './model-measurements.component';
import { ModelDetailPublicDto } from '../../../../../../../core/services/public-model.service';
import { TranslationService } from '../../../../../../../core/services/translation.service';

describe('ModelMeasurementsComponent', () => {
  let component: ModelMeasurementsComponent;
  let fixture: ComponentFixture<ModelMeasurementsComponent>;

  const mockDetail: ModelDetailPublicDto = {
    id: 'test-1',
    stageName: 'ISABELLA M',
    gender: 'FEMALE',
    heightCm: 179,
    bustChestCm: 84 as any,
    waistCm: 60 as any,
    hipsCm: 90 as any,
    dressSize: '36',
    shoeSize: '38',
    eyeColor: 'Verdes',
    hairColor: 'Castanho'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModelMeasurementsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        TranslationService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ModelMeasurementsComponent);
    component = fixture.componentInstance;
  });

  it('should create and render all filled measurements', () => {
    fixture.componentRef.setInput('model', mockDetail);
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.measurementList().length).toBe(8);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('179 cm');
    expect(compiled.textContent).toContain('84 cm');
    expect(compiled.textContent).toContain('60 cm');
    expect(compiled.textContent).toContain('90 cm');
    expect(compiled.textContent).toContain('36');
    expect(compiled.textContent).toContain('38');
    expect(compiled.textContent).toContain('Verdes');
    expect(compiled.textContent).toContain('Castanho');
  });

  it('should suppress null or empty fields', () => {
    const partialDetail: ModelDetailPublicDto = {
      id: 'test-2',
      stageName: 'LUCAS B',
      gender: 'MALE',
      heightCm: 188,
      shoeSize: '42'
      // All other measurements are undefined
    };

    fixture.componentRef.setInput('model', partialDetail);
    fixture.detectChanges();

    expect(component.measurementList().length).toBe(2);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('188 cm');
    expect(compiled.textContent).toContain('42');
    expect(compiled.textContent).not.toContain('Busto');
  });
});
