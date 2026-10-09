import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HomeHeroComponent } from './home-hero.component';
import { environment } from '../../../../../../environments/environment';

describe('HomeHeroComponent', () => {
  let component: HomeHeroComponent;
  let fixture: ComponentFixture<HomeHeroComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeHeroComponent, HttpClientTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeHeroComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function flushPublicSettings(data: any = null) {
    const reqs = httpMock.match((r) => r.url.startsWith(`${environment.apiUrl}/home-settings`));
    reqs.forEach((r) => r.flush(data || {}));
  }

  it('should create home hero component', () => {
    fixture.detectChanges();
    flushPublicSettings();
    expect(component).toBeTruthy();
  });

  it('should display transparent splash logo initially and remove it after exactly 1000ms', fakeAsync(() => {
    fixture.detectChanges();
    flushPublicSettings();

    expect(component.showSplashLogo()).toBeTrue();

    let compiled = fixture.nativeElement as HTMLElement;
    const splashImg = compiled.querySelector('img[alt="WB Agency"]') as HTMLImageElement;
    expect(splashImg).toBeTruthy();
    expect(splashImg.src).toContain('assets/images/logo-wb-agency-light.png');
    expect(splashImg.classList.contains('bg-transparent')).toBeTrue();
    expect(splashImg.classList.contains('object-contain')).toBeTrue();

    // Garante que NÃO existam botões CTA centrais na primeira dobra
    expect(compiled.querySelector('.hero-buttons')).toBeNull();
    expect(compiled.querySelector('button.btn-primary')).toBeNull();
    expect(compiled.textContent).not.toContain('Ver Elenco');
    expect(compiled.textContent).not.toContain('Seja Modelo');

    // Avança o temporizador em 1000ms
    tick(1000);
    fixture.detectChanges();

    // Logo desaparece deixando o vídeo livre
    expect(component.showSplashLogo()).toBeFalse();
    compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('img[alt="WB Agency"]')).toBeNull();
  }));

  it('should customize splash logo duration according to server settings', fakeAsync(() => {
    fixture.detectChanges();
    flushPublicSettings({ splashDurationMs: 2500 });

    expect(component.showSplashLogo()).toBeTrue();

    tick(1500);
    fixture.detectChanges();
    // Aos 1500ms ainda deve estar ativo (pois configurado para 2500ms)
    expect(component.showSplashLogo()).toBeTrue();

    tick(1000);
    fixture.detectChanges();
    // Aos 2500ms deve encerrar
    expect(component.showSplashLogo()).toBeFalse();
  }));

  it('should contain looping background video element and bind dynamic poster and video', () => {
    fixture.detectChanges();
    flushPublicSettings({
      videoUrl: 'https://cdn.wb.agency/custom-video.mp4',
      posterUrl: 'https://cdn.wb.agency/custom-poster.jpg'
    });
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const video = compiled.querySelector('video') as HTMLVideoElement;
    expect(video).toBeTruthy();
    expect(video.hasAttribute('loop') || video.loop).toBeTrue();
    expect(video.hasAttribute('muted') || video.muted).toBeTrue();
    expect(video.getAttribute('poster')).toBe('https://cdn.wb.agency/custom-poster.jpg');

    const source = video.querySelector('source') as HTMLSourceElement;
    expect(source.getAttribute('src')).toBe('https://cdn.wb.agency/custom-video.mp4');
  });

  it('should render audio toggle button and toggle mute state on click', () => {
    fixture.detectChanges();
    flushPublicSettings();
    const compiled = fixture.nativeElement as HTMLElement;
    const audioBtn = compiled.querySelector('.audio-toggle-btn') as HTMLButtonElement;
    expect(audioBtn).toBeTruthy();
    expect(audioBtn.textContent).toContain('SOUND OFF');
    expect(component.isMuted()).toBeTrue();

    // Clica para ativar o som
    audioBtn.click();
    fixture.detectChanges();

    expect(component.isMuted()).toBeFalse();
    expect(audioBtn.textContent).toContain('SOUND ON');

    // Clica para silenciar novamente
    audioBtn.click();
    fixture.detectChanges();

    expect(component.isMuted()).toBeTrue();
    expect(audioBtn.textContent).toContain('SOUND OFF');
  });

  it('should render scroll down button and trigger scrollToContent', () => {
    spyOn(window, 'scrollTo');
    fixture.detectChanges();
    flushPublicSettings();
    const compiled = fixture.nativeElement as HTMLElement;
    const scrollBtn = compiled.querySelector('.scroll-down-btn') as HTMLButtonElement;
    expect(scrollBtn).toBeTruthy();
    expect(scrollBtn.textContent).toContain('SCROLL');

    scrollBtn.click();
    expect(window.scrollTo).toHaveBeenCalled();
  });

  it('should display floating disclaimer modal when active and allow closing it', () => {
    fixture.detectChanges();
    flushPublicSettings({
      disclaimerActive: true,
      disclaimerTitle: 'AVISO IMPORTANTE',
      disclaimerText: 'Temporada de inscrições internacionais aberta.',
      disclaimerLinkUrl: 'https://wb.agency/apply',
      disclaimerLinkLabel: 'INSCREVER-SE'
    });
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('AVISO IMPORTANTE');
    expect(compiled.textContent).toContain('Temporada de inscrições internacionais aberta.');
    expect(compiled.textContent).toContain('INSCREVER-SE');

    const closeBtn = compiled.querySelector('button[aria-label="Fechar Aviso"]') as HTMLButtonElement;
    expect(closeBtn).toBeTruthy();

    closeBtn.click();
    fixture.detectChanges();

    expect(component.showDisclaimerModal).toBeFalse();
    expect(compiled.querySelector('button[aria-label="Fechar Aviso"]')).toBeNull();
  });
});
