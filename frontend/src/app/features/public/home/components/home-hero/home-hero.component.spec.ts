import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HomeHeroComponent } from './home-hero.component';

describe('HomeHeroComponent', () => {
  let component: HomeHeroComponent;
  let fixture: ComponentFixture<HomeHeroComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeHeroComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeHeroComponent);
    component = fixture.componentInstance;
  });

  it('should create home hero component', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should display transparent splash logo initially and remove it after exactly 1000ms', fakeAsync(() => {
    fixture.detectChanges();

    expect(component.showSplashLogo()).toBeTrue();

    let compiled = fixture.nativeElement as HTMLElement;
    const splashImg = compiled.querySelector('img[alt="WB Agency"]') as HTMLImageElement;
    expect(splashImg).toBeTruthy();
    expect(splashImg.src).toContain('assets/images/logo-wb-agency-light.png');
    expect(splashImg.classList.contains('bg-transparent')).toBeTrue();
    expect(splashImg.classList.contains('object-contain')).toBeTrue();

    // Garante que NÃO existam botões na primeira dobra
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

  it('should contain looping background video element', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const video = compiled.querySelector('video') as HTMLVideoElement;
    expect(video).toBeTruthy();
    expect(video.hasAttribute('loop') || video.loop).toBeTrue();
    expect(video.hasAttribute('muted') || video.muted).toBeTrue();
  });
});
