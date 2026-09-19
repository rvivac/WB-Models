import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError, BehaviorSubject } from 'rxjs';
import { ModelDetailComponent } from './model-detail.component';
import { PublicModelService, ModelDetailPublicDto } from '../../../../../core/services/public-model.service';
import { TranslationService } from '../../../../../core/services/translation.service';

describe('ModelDetailComponent', () => {
  let component: ModelDetailComponent;
  let fixture: ComponentFixture<ModelDetailComponent>;
  let publicModelService: PublicModelService;

  const mockModelDetail: ModelDetailPublicDto = {
    id: 'uuid-123',
    stageName: 'VALENTINA R',
    gender: 'FEMALE',
    isStar: true,
    city: 'Curitiba',
    nationality: 'Brasileira',
    age: 22,
    instagramUrl: 'https://instagram.com/valentinar',
    heightCm: 180,
    bustChestCm: 86 as any,
    waistCm: 61 as any,
    hipsCm: 91 as any,
    dressSize: '36',
    shoeSize: '38',
    eyeColor: 'Castanhos',
    hairColor: 'Castanho Escuro',
    bookPhotos: [
      { id: 'b1', fileUrl: 'https://example.com/b1.jpg', displayOrder: 1, isCover: true },
      { id: 'b2', fileUrl: 'https://example.com/b2.jpg', displayOrder: 2, isCover: false }
    ],
    polaroids: [
      { id: 'p1', fileUrl: 'https://example.com/p1.jpg', displayOrder: 1, isCover: false }
    ],
    composite: {
      id: 'comp-1',
      fileUrl: 'https://example.com/composite.pdf'
    }
  };

  const paramMapSubject = new BehaviorSubject<{ id?: string }>({ id: 'uuid-123' });

  beforeEach(async () => {
    paramMapSubject.next({ id: 'uuid-123' });

    await TestBed.configureTestingModule({
      imports: [ModelDetailComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        PublicModelService,
        TranslationService,
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: paramMapSubject.pipe(
              // Simula o ParamMap do Angular
              (obs) => of({
                get: (key: string) => (key === 'id' ? 'uuid-123' : null),
                has: (key: string) => key === 'id'
              })
            )
          }
        }
      ]
    }).compileComponents();

    publicModelService = TestBed.inject(PublicModelService);
  });

  it('should create and display loaded model details with star badge and composite button', () => {
    spyOn(publicModelService, 'getModelById').and.returnValue(of(mockModelDetail));

    fixture = TestBed.createComponent(ModelDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.isLoading()).toBeFalse();
    expect(component.model()?.stageName).toBe('VALENTINA R');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.profile-name')?.textContent).toContain('VALENTINA R');
    expect(compiled.querySelector('.star-badge')).toBeTruthy();
    expect(compiled.querySelector('.btn-composite')).toBeTruthy();
    expect(compiled.querySelectorAll('.photo-card').length).toBe(2); // 2 fotos no book
  });

  it('should switch tabs between book and polaroids', () => {
    spyOn(publicModelService, 'getModelById').and.returnValue(of(mockModelDetail));

    fixture = TestBed.createComponent(ModelDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.activeTab()).toBe('BOOK');
    expect(component.currentGalleryPhotos().length).toBe(2);

    component.setTab('POLAROIDS');
    fixture.detectChanges();

    expect(component.activeTab()).toBe('POLAROIDS');
    expect(component.currentGalleryPhotos().length).toBe(1);
  });

  it('should open and close lightbox', () => {
    spyOn(publicModelService, 'getModelById').and.returnValue(of(mockModelDetail));

    fixture = TestBed.createComponent(ModelDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isLightboxOpen()).toBeFalse();

    component.openLightbox(1);
    expect(component.lightboxIndex()).toBe(1);
    expect(component.isLightboxOpen()).toBeTrue();

    component.closeLightbox();
    expect(component.lightboxIndex()).toBe(-1);
    expect(component.isLightboxOpen()).toBeFalse();
  });

  it('should display 404 not found state when API returns error', () => {
    spyOn(publicModelService, 'getModelById').and.returnValue(
      throwError(() => new Error('Not found'))
    );

    fixture = TestBed.createComponent(ModelDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isLoading()).toBeFalse();
    expect(component.isNotFound()).toBeTrue();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.not-found-state')).toBeTruthy();
  });
});
