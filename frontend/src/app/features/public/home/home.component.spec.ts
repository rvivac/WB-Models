import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HomeComponent } from './home.component';
import { TranslationService } from '../../../core/services/translation.service';
import { PublicContentService } from '../../../core/services/public-content.service';
import { PublicModelService } from '../../../core/services/public-model.service';

describe('HomeComponent - Vitrine Stars', () => {
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
        PublicContentService,
        PublicModelService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve instanciar o componente e não renderizar o elemento app-home-featured-models', () => {
    expect(component).toBeTruthy();
    const featuredSection = fixture.debugElement.query(By.css('app-home-featured-models'));
    expect(featuredSection).withContext('O seletor da vitrine Stars não deve estar presente no DOM da Home').toBeNull();
  });
});
