import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
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
      title: 'High Fashion & Scouting',
      subtitle: 'Gestão de Carreiras • Scouting Internacional',
      ctaText: 'Ver Elenco',
      ctaLink: '/models/female'
    }));

    fixture = TestBed.createComponent(HomeHeroComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and display hero video without central text overlay', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('video')).toBeTruthy();
    expect(compiled.querySelector('.hero-headline')).toBeNull();
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

  it('should initialize with audio muted, volume 0 and TARGET_VOLUME of 0.05', () => {
    expect(component.isMuted).toBeTrue();
    expect(component.TARGET_VOLUME).toBe(0.05);

    const videoElement = component.heroVideo.nativeElement;
    expect(videoElement.muted).toBeTrue();
    expect(videoElement.volume).toBe(0);

    const audioBtn = fixture.nativeElement.querySelector('.audio-toggle-btn');
    expect(audioBtn).toBeTruthy();
    expect(audioBtn.textContent).toContain('SOUND OFF');
    expect(audioBtn.getAttribute('aria-pressed')).toBe('false');
  });

  it('should toggle audio mute state, perform gradual fade-in to TARGET_VOLUME, and zero volume on mute', fakeAsync(() => {
    const videoElement = component.heroVideo.nativeElement;
    expect(videoElement.muted).toBeTrue();
    expect(videoElement.volume).toBe(0);

    // Toggle to Unmute
    component.toggleAudio();
    fixture.detectChanges();

    expect(component.isMuted).toBeFalse();
    expect(videoElement.muted).toBeFalse();
    expect(videoElement.volume).toBe(0);

    // Avança 50ms no fade-in (0 -> 0.02)
    tick(50);
    expect(videoElement.volume).toBe(0.02);

    // Avança mais 50ms (0.02 -> 0.04)
    tick(50);
    expect(videoElement.volume).toBe(0.04);

    // Avança mais 50ms (atinge TARGET_VOLUME 0.05 e para o intervalo)
    tick(50);
    expect(videoElement.volume).toBe(0.05);

    const audioBtn = fixture.nativeElement.querySelector('.audio-toggle-btn');
    expect(audioBtn.textContent).toContain('SOUND ON');
    expect(audioBtn.getAttribute('aria-pressed')).toBe('true');

    // Toggle back to Mute (muta e zera o volume imediatamente)
    component.toggleAudio();
    fixture.detectChanges();

    expect(component.isMuted).toBeTrue();
    expect(videoElement.muted).toBeTrue();
    expect(videoElement.volume).toBe(0);
    expect(audioBtn.textContent).toContain('SOUND OFF');
  }));

  it('should hide audio button when video encounters an error', () => {
    component.onVideoError();
    fixture.detectChanges();

    const audioBtn = fixture.nativeElement.querySelector('.audio-toggle-btn');
    expect(audioBtn).toBeNull();
  });
});
