import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HomeComponent } from './home.component';
import { TranslationService } from '../../../core/services/translation.service';
import { PublicContentService } from '../../../core/services/public-content.service';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        TranslationService,
        PublicContentService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create home page with hero component and featured models', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-home-hero')).toBeTruthy();
    expect(compiled.querySelector('.manifesto-section')).toBeTruthy();
    expect(compiled.querySelector('.featured-section')).toBeTruthy();
    expect(compiled.querySelectorAll('.model-card').length).toBe(3);
  });
});
