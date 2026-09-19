import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { HomeFeaturedModelsComponent } from './home-featured-models.component';
import { PublicModelService, ModelCardPublicDto, PageResponseDto } from '../../../../../core/services/public-model.service';
import { TranslationService } from '../../../../../core/services/translation.service';

describe('HomeFeaturedModelsComponent', () => {
  let component: HomeFeaturedModelsComponent;
  let fixture: ComponentFixture<HomeFeaturedModelsComponent>;
  let publicModelService: PublicModelService;

  const mockModels: ModelCardPublicDto[] = [
    {
      id: 'm1',
      stageName: 'Gisele V',
      gender: 'FEMALE',
      coverImageUrl: 'https://images.example.com/gisele.jpg',
      heightCm: 179,
      city: 'São Paulo',
      isStar: true
    },
    {
      id: 'm2',
      stageName: 'Lucas B',
      gender: 'MALE',
      coverImageUrl: '',
      heightCm: 188,
      city: 'Rio de Janeiro',
      isStar: false
    }
  ];

  const mockPageResponse: PageResponseDto<ModelCardPublicDto> = {
    content: mockModels,
    pageNumber: 0,
    pageSize: 8,
    totalElements: 2,
    totalPages: 1,
    last: true,
    isLast: true
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeFeaturedModelsComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        PublicModelService,
        TranslationService
      ]
    }).compileComponents();

    publicModelService = TestBed.inject(PublicModelService);
  });

  it('should create and load featured models with star badge', () => {
    spyOn(publicModelService, 'getFeaturedModels').and.returnValue(of(mockPageResponse));

    fixture = TestBed.createComponent(HomeFeaturedModelsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.isLoading()).toBeFalse();
    expect(component.models().length).toBe(2);

    const compiled = fixture.nativeElement as HTMLElement;
    const cards = compiled.querySelectorAll('.model-card');
    expect(cards.length).toBe(2);

    // Primeiro modelo tem isStar = true
    const starBadge = cards[0].querySelector('.star-badge');
    expect(starBadge).toBeTruthy();

    // Segundo modelo tem isStar = false
    const secondStarBadge = cards[1].querySelector('.star-badge');
    expect(secondStarBadge).toBeFalsy();
  });

  it('should fallback to default image when coverImageUrl is empty', () => {
    spyOn(publicModelService, 'getFeaturedModels').and.returnValue(of(mockPageResponse));

    fixture = TestBed.createComponent(HomeFeaturedModelsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const img = component.getCoverImage(mockModels[1]);
    expect(img).toBe(component.defaultCoverImage);
  });

  it('should render empty state when models list is empty', () => {
    spyOn(publicModelService, 'getFeaturedModels').and.returnValue(
      of({
        content: [],
        pageNumber: 0,
        pageSize: 8,
        totalElements: 0,
        totalPages: 0,
        last: true,
        isLast: true
      })
    );

    fixture = TestBed.createComponent(HomeFeaturedModelsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.hasModels()).toBeFalse();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-state')).toBeTruthy();
  });

  it('should handle API errors gracefully', () => {
    spyOn(publicModelService, 'getFeaturedModels').and.returnValue(
      throwError(() => new Error('Server error'))
    );

    fixture = TestBed.createComponent(HomeFeaturedModelsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isLoading()).toBeFalse();
    expect(component.hasError()).toBeTrue();
    expect(component.models().length).toBe(0);
  });
});
