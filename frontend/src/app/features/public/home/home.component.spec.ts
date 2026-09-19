import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { HomeComponent } from './home.component';
import { TranslationService } from '../../../core/services/translation.service';
import { PublicContentService } from '../../../core/services/public-content.service';
import { PublicModelService } from '../../../core/services/public-model.service';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let publicModelService: PublicModelService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        TranslationService,
        PublicContentService,
        PublicModelService
      ]
    }).compileComponents();

    publicModelService = TestBed.inject(PublicModelService);
    spyOn(publicModelService, 'getFeaturedModels').and.returnValue(of({
      content: [
        {
          id: 'model-1',
          stageName: 'ISABELLA M',
          gender: 'FEMALE',
          coverImageUrl: 'https://example.com/cover.jpg',
          heightCm: 179,
          city: 'São Paulo',
          isStar: true
        }
      ],
      pageNumber: 0,
      pageSize: 8,
      totalElements: 1,
      totalPages: 1,
      last: true,
      isLast: true
    }));

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create home page with hero component and featured models section', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-home-hero')).toBeTruthy();
    expect(compiled.querySelector('app-home-featured-models')).toBeTruthy();
    expect(compiled.querySelector('.manifesto-section')).toBeTruthy();
    expect(compiled.querySelector('.cta-banner')).toBeTruthy();
  });
});
