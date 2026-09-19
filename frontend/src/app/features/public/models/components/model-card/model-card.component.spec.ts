import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ModelCardComponent } from './model-card.component';
import { ModelCardPublicDto } from '../../../../../core/services/public-model.service';
import { TranslationService } from '../../../../../core/services/translation.service';

describe('ModelCardComponent', () => {
  let component: ModelCardComponent;
  let fixture: ComponentFixture<ModelCardComponent>;

  const mockModel: ModelCardPublicDto = {
    id: 'test-uuid-1',
    stageName: 'VALENTINA R',
    gender: 'FEMALE',
    coverImageUrl: 'https://images.example.com/valentina.jpg',
    heightCm: 180,
    city: 'Curitiba',
    isStar: true
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModelCardComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        TranslationService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ModelCardComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('model', mockModel);
    fixture.detectChanges();
  });

  it('should create and render model stageName, height and city', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.model-name')?.textContent).toContain('VALENTINA R');
    expect(compiled.querySelector('.metric-item')?.textContent).toContain('180 cm');
    expect(compiled.textContent).toContain('Curitiba');
  });

  it('should render star badge when isStar is true', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.star-badge')).toBeTruthy();
  });

  it('should fallback to default image on image error', () => {
    expect(component.isImageError()).toBeFalse();
    component.onImageError();
    fixture.detectChanges();

    expect(component.isImageError()).toBeTrue();
    expect(component.getImageSrc()).toBe(component.defaultCoverImage);
  });
});
