import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { ModelSpotlightShowcaseComponent } from './model-spotlight-showcase.component';
import { PublicModelService, ModelCardPublicDto, PageResponseDto } from '../../../../../core/services/public-model.service';
import { TranslationService } from '../../../../../core/services/translation.service';

describe('ModelSpotlightShowcaseComponent', () => {
  let component: ModelSpotlightShowcaseComponent;
  let fixture: ComponentFixture<ModelSpotlightShowcaseComponent>;
  let publicModelService: PublicModelService;

  const mockModels: ModelCardPublicDto[] = [
    {
      id: 'spot-1',
      stageName: 'ISABELLA MARTINS',
      gender: 'FEMALE',
      coverImageUrl: 'https://example.com/isabella.jpg',
      heightCm: 179,
      city: 'São Paulo',
      isStar: true
    },
    {
      id: 'spot-2',
      stageName: 'GABRIEL ALENCAR',
      gender: 'MALE',
      coverImageUrl: 'https://example.com/gabriel.jpg',
      heightCm: 188,
      city: 'Rio de Janeiro',
      isStar: true
    },
    {
      id: 'spot-3',
      stageName: 'HELENA VASCONCELOS',
      gender: 'FEMALE',
      coverImageUrl: '',
      heightCm: 177,
      city: 'Belo Horizonte',
      isStar: true
    }
  ];

  const mockResponse: PageResponseDto<ModelCardPublicDto> = {
    content: mockModels,
    pageNumber: 0,
    pageSize: 6,
    totalElements: 3,
    totalPages: 1,
    isLast: true
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModelSpotlightShowcaseComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        PublicModelService,
        TranslationService
      ]
    }).compileComponents();

    publicModelService = TestBed.inject(PublicModelService);
    spyOn(publicModelService, 'getModels').and.returnValue(of(mockResponse));

    fixture = TestBed.createComponent(ModelSpotlightShowcaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    component.stopAutoPlay();
  });

  it('should create and display spotlight models', () => {
    expect(component).toBeTruthy();
    expect(component.isLoading()).toBeFalse();
    expect(component.models().length).toBe(3);
    expect(component.currentIndex()).toBe(0);

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('ISABELLA MARTINS');
  });

  it('should advance slide on nextSlide and wrap around on boundaries', () => {
    expect(component.currentIndex()).toBe(0);

    component.nextSlide();
    expect(component.currentIndex()).toBe(1);

    component.nextSlide();
    expect(component.currentIndex()).toBe(2);

    // Wrap around to 0
    component.nextSlide();
    expect(component.currentIndex()).toBe(0);
  });

  it('should retreat slide on prevSlide and wrap around', () => {
    expect(component.currentIndex()).toBe(0);

    // Wrap around to last item (2)
    component.prevSlide();
    expect(component.currentIndex()).toBe(2);

    component.prevSlide();
    expect(component.currentIndex()).toBe(1);
  });

  it('should navigate to specific slide via goToSlide', () => {
    component.goToSlide(2);
    expect(component.currentIndex()).toBe(2);
  });

  it('should pause and resume autoplay on hover', () => {
    expect(component.isHovered()).toBeFalse();

    component.pauseAutoPlay();
    expect(component.isHovered()).toBeTrue();

    component.resumeAutoPlay();
    expect(component.isHovered()).toBeFalse();
  });

  it('should advance slide on touch swipe left', () => {
    expect(component.currentIndex()).toBe(0);

    // Simula touch swipe para a esquerda (diff < -45)
    component.onTouchStart({
      touches: [{ clientX: 200 } as any]
    } as unknown as TouchEvent);

    component.onTouchEnd({
      changedTouches: [{ clientX: 100 } as any]
    } as unknown as TouchEvent);

    expect(component.currentIndex()).toBe(1);
  });

  it('should retreat slide on touch swipe right', () => {
    component.goToSlide(1);

    // Simula touch swipe para a direita (diff > 45)
    component.onTouchStart({
      touches: [{ clientX: 100 } as any]
    } as unknown as TouchEvent);

    component.onTouchEnd({
      changedTouches: [{ clientX: 200 } as any]
    } as unknown as TouchEvent);

    expect(component.currentIndex()).toBe(0);
  });
});
