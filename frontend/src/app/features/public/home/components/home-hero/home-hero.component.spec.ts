import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { HomeHeroComponent } from './home-hero.component';
import { PublicContentService } from '../../../../../core/services/public-content.service';
import { TranslationService } from '../../../../../core/services/translation.service';

describe('HomeHeroComponent', () => {
  let component: HomeHeroComponent;
  let fixture: ComponentFixture<HomeHeroComponent>;
  let publicContentService: PublicContentService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeHeroComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        PublicContentService,
        TranslationService
      ]
    }).compileComponents();

    publicContentService = TestBed.inject(PublicContentService);
    spyOn(publicContentService, 'getHeroContent').and.returnValue(of({
      videoUrl: 'https://video.mp4',
      posterImageUrl: 'https://poster.jpg',
      title: 'WB SCOUTING',
      subtitle: 'Manifesto editorial de moda',
      ctaText: 'Ver Casting',
      ctaLink: '/models/female'
    }));

    fixture = TestBed.createComponent(HomeHeroComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and display hero typography and video', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.hero-headline')?.textContent).toContain('WB SCOUTING');
    expect(compiled.querySelector('video')).toBeTruthy();
  });

  it('should trigger poster fallback when video encounters error', () => {
    component.onVideoError();
    fixture.detectChanges();

    expect(component.isVideoError()).toBeTrue();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.hero-poster-fallback')).toBeTruthy();
  });

  it('should invoke window.scrollTo when scrollToContent is called', () => {
    spyOn(window, 'scrollTo');
    component.scrollToContent();
    expect(window.scrollTo).toHaveBeenCalled();
  });
});
