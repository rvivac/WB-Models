import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HeaderComponent } from './header.component';
import { TranslationService } from '../../../core/services/translation.service';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        TranslationService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create header component with navigation links and locked brand logo dimensions', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    const logoImg = compiled.querySelector('.brand-logo-img') as HTMLImageElement;
    expect(logoImg).toBeTruthy();
    expect(logoImg.classList.contains('brand-logo-header')).toBeTrue();
    expect(logoImg.style.height).toBe('48px');
    expect(logoImg.style.maxHeight).toBe('48px');
    expect(logoImg.getAttribute('width')).toBe('765');
    expect(logoImg.getAttribute('height')).toBe('507');
    expect(compiled.querySelector('.desktop-nav')).toBeTruthy();
  });

  it('should react to window scroll event', () => {
    expect(component.isScrolled()).toBeFalse();
    
    // Simular scrollY > 50
    Object.defineProperty(window, 'scrollY', { value: 100, writable: true });
    window.dispatchEvent(new Event('scroll'));
    
    expect(component.isScrolled()).toBeTrue();
  });

  it('should toggle and close mobile menu', () => {
    component.toggleMenu();
    expect(component.themeService.isMobileMenuOpen()).toBeTrue();

    component.closeMenu();
    expect(component.themeService.isMobileMenuOpen()).toBeFalse();
  });
});
