import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FooterComponent } from './footer.component';

describe('FooterComponent', () => {
  let component: FooterComponent;
  let fixture: ComponentFixture<FooterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FooterComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FooterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create footer with current year and brand logo', () => {
    expect(component).toBeTruthy();
    expect(component.currentYear).toBe(new Date().getFullYear());
    const compiled = fixture.nativeElement as HTMLElement;
    const logoImg = compiled.querySelector('img');
    expect(logoImg?.getAttribute('alt')).toBe('WB Agency');
    expect(logoImg?.getAttribute('src')).toBe('assets/images/logo-wb-agency.jpeg');
  });

  it('should render navigation links and contact info', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Casting & Modelos');
    expect(compiled.textContent).toContain('contato@wbscouting.com');
    expect(compiled.textContent).toContain('PARIS • MILAN • NEW YORK • SÃO PAULO');
  });
});
