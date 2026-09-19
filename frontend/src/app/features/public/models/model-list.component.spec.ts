import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router, ActivatedRoute } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, BehaviorSubject } from 'rxjs';
import { ModelListComponent } from './model-list.component';
import { PublicModelService, ModelCardPublicDto, PageResponseDto } from '../../../core/services/public-model.service';
import { TranslationService } from '../../../core/services/translation.service';

describe('ModelListComponent', () => {
  let component: ModelListComponent;
  let fixture: ComponentFixture<ModelListComponent>;
  let publicModelService: PublicModelService;
  let router: Router;

  const mockModels: ModelCardPublicDto[] = [
    {
      id: 'mod-1',
      stageName: 'ISABELLA M',
      gender: 'FEMALE',
      coverImageUrl: 'https://images.example.com/cover1.jpg',
      heightCm: 179,
      city: 'São Paulo',
      isStar: true
    },
    {
      id: 'mod-2',
      stageName: 'CAMILA R',
      gender: 'FEMALE',
      coverImageUrl: 'https://images.example.com/cover2.jpg',
      heightCm: 181,
      city: 'Florianópolis',
      isStar: false
    }
  ];

  const mockPageResponse: PageResponseDto<ModelCardPublicDto> = {
    content: mockModels,
    pageNumber: 0,
    pageSize: 24,
    totalElements: 2,
    totalPages: 1,
    isLast: true
  };

  const routeDataSubject = new BehaviorSubject<{ gender?: string; isStar?: boolean }>({ gender: 'FEMALE' });
  const queryParamsSubject = new BehaviorSubject<Record<string, any>>({});

  beforeEach(async () => {
    routeDataSubject.next({ gender: 'FEMALE', isStar: false });
    queryParamsSubject.next({});

    await TestBed.configureTestingModule({
      imports: [ModelListComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        PublicModelService,
        TranslationService,
        {
          provide: ActivatedRoute,
          useValue: {
            data: routeDataSubject.asObservable(),
            queryParams: queryParamsSubject.asObservable(),
            snapshot: {
              data: { gender: 'FEMALE' },
              queryParams: {}
            }
          }
        }
      ]
    }).compileComponents();

    publicModelService = TestBed.inject(PublicModelService);
    router = TestBed.inject(Router);
    spyOn(publicModelService, 'getModels').and.returnValue(of(mockPageResponse));
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(ModelListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load female casting with 2 models', () => {
    expect(component).toBeTruthy();
    expect(component.isLoading()).toBeFalse();
    expect(component.models().length).toBe(2);
    expect(component.headerTitleKey()).toBe('models.female_title');

    const compiled = fixture.nativeElement as HTMLElement;
    const cards = compiled.querySelectorAll('app-model-card');
    expect(cards.length).toBe(2);
  });

  it('should switch header to stars when isStar is present in route data', () => {
    routeDataSubject.next({ isStar: true });
    fixture.detectChanges();

    expect(component.isStarCategory()).toBeTrue();
    expect(component.headerTitleKey()).toBe('models.stars_title');
  });

  it('should switch header to male when gender MALE is present in route data', () => {
    routeDataSubject.next({ gender: 'MALE', isStar: false });
    fixture.detectChanges();

    expect(component.currentGender()).toBe('MALE');
    expect(component.headerTitleKey()).toBe('models.male_title');
  });

  it('should update queryParams on filter change', () => {
    component.onFilterChange({
      search: 'Isabella',
      sort: 'createdAt,desc'
    });

    expect(router.navigate).toHaveBeenCalledWith([], {
      relativeTo: jasmine.any(Object),
      queryParams: {
        search: 'Isabella',
        sort: 'createdAt,desc',
        page: null
      },
      queryParamsHandling: 'merge'
    });
  });

  it('should update queryParams on reset filters', () => {
    component.onResetFilters();

    expect(router.navigate).toHaveBeenCalledWith([], {
      relativeTo: jasmine.any(Object),
      queryParams: {
        search: null,
        sort: null,
        page: null
      },
      queryParamsHandling: 'merge'
    });
  });

  it('should navigate to valid page on goToPage', () => {
    // Set totalPages to 3
    component['totalPages'].set(3);
    component.goToPage(2);

    expect(router.navigate).toHaveBeenCalledWith([], {
      relativeTo: jasmine.any(Object),
      queryParams: {
        page: 2
      },
      queryParamsHandling: 'merge'
    });
  });
});
