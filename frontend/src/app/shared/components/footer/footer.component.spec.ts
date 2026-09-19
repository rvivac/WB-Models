import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { FooterComponent } from './footer.component';
import { PublicContentService } from '../../../core/services/public-content.service';
import { TranslationService } from '../../../core/services/translation.service';

describe('FooterComponent', () => {
  let component: FooterComponent;
  let fixture: ComponentFixture<FooterComponent>;
  let publicContentService: PublicContentService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FooterComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        PublicContentService,
        TranslationService
      ]
    }).compileComponents();

    publicContentService = TestBed.inject(PublicContentService);
    spyOn(publicContentService, 'getContactChannels').and.returnValue(of({
      email: 'teste@wbscouting.com',
      whatsappNumber: '5511988887777',
      whatsappUrl: 'https://wa.me/5511988887777',
      instagramHandle: '@wbscouting',
      address: 'São Paulo - SP',
      officeHours: '09h às 18h'
    }));

    fixture = TestBed.createComponent(FooterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create footer with brand title and contact channels', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand-title')?.textContent).toContain('WB SCOUTING');
    expect(compiled.querySelector('.whatsapp-link')?.getAttribute('href')).toBe('https://wa.me/5511988887777');
  });

  it('should format instagram url correctly', () => {
    const url = component.getInstagramUrl();
    expect(url).toBe('https://instagram.com/wbscouting');
  });
});
