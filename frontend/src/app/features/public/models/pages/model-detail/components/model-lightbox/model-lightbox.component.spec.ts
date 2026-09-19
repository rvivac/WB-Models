import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ModelLightboxComponent } from './model-lightbox.component';
import { TranslationService } from '../../../../../../../core/services/translation.service';

describe('ModelLightboxComponent', () => {
  let component: ModelLightboxComponent;
  let fixture: ComponentFixture<ModelLightboxComponent>;

  const mockImages = [
    'https://example.com/photo1.jpg',
    'https://example.com/photo2.jpg',
    'https://example.com/photo3.jpg'
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModelLightboxComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        TranslationService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ModelLightboxComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('images', mockImages);
    fixture.componentRef.setInput('currentIndex', 0);
    fixture.detectChanges();
  });

  it('should create and display first image', () => {
    expect(component).toBeTruthy();
    expect(component.currentImageUrl()).toBe(mockImages[0]);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.lightbox-counter')?.textContent).toContain('1 / 3');
  });

  it('should emit close on Escape keydown', () => {
    spyOn(component.close, 'emit');
    const event = new KeyboardEvent('keydown', { key: 'Escape' });
    window.dispatchEvent(event);

    expect(component.close.emit).toHaveBeenCalled();
  });

  it('should emit next index on ArrowRight', () => {
    spyOn(component.indexChange, 'emit');
    const event = new KeyboardEvent('keydown', { key: 'ArrowRight' });
    window.dispatchEvent(event);

    expect(component.indexChange.emit).toHaveBeenCalledWith(1);
  });

  it('should emit previous index on ArrowLeft with wrap around', () => {
    spyOn(component.indexChange, 'emit');
    const event = new KeyboardEvent('keydown', { key: 'ArrowLeft' });
    window.dispatchEvent(event);

    // Current index is 0, wrap around to last index (2)
    expect(component.indexChange.emit).toHaveBeenCalledWith(2);
  });
});
