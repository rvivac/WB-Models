import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ModelCompositeModalComponent } from './model-composite-modal.component';
import { TranslationService } from '../../../../../../../core/services/translation.service';

describe('ModelCompositeModalComponent', () => {
  let component: ModelCompositeModalComponent;
  let fixture: ComponentFixture<ModelCompositeModalComponent>;
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModelCompositeModalComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        TranslationService
      ]
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ModelCompositeModalComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should lock body scroll on init and restore on destroy', () => {
    fixture.componentRef.setInput('compositeUrl', 'https://example.com/composite.jpg');
    fixture.componentRef.setInput('modelName', 'Adriana Lima');
    fixture.detectChanges();

    expect(document.body.style.overflow).toBe('hidden');

    fixture.destroy();
    expect(document.body.style.overflow).not.toBe('hidden');
  });

  it('should emit close on Escape keydown', () => {
    fixture.componentRef.setInput('compositeUrl', 'https://example.com/composite.jpg');
    fixture.componentRef.setInput('modelName', 'Adriana Lima');
    fixture.detectChanges();

    const closeSpy = spyOn(component.close, 'emit');

    const event = new KeyboardEvent('keydown', { key: 'Escape' });
    window.dispatchEvent(event);

    expect(closeSpy).toHaveBeenCalled();
  });

  it('should emit close when close button is clicked', () => {
    fixture.componentRef.setInput('compositeUrl', 'https://example.com/composite.jpg');
    fixture.componentRef.setInput('modelName', 'Adriana Lima');
    fixture.detectChanges();

    const closeSpy = spyOn(component.close, 'emit');
    const compiled = fixture.nativeElement as HTMLElement;
    const closeBtn = compiled.querySelector('.btn-close') as HTMLButtonElement;
    expect(closeBtn).toBeTruthy();

    closeBtn.click();
    expect(closeSpy).toHaveBeenCalled();
  });

  it('should emit close on backdrop click outside the image', () => {
    fixture.componentRef.setInput('compositeUrl', 'https://example.com/composite.jpg');
    fixture.componentRef.setInput('modelName', 'Adriana Lima');
    fixture.detectChanges();

    const closeSpy = spyOn(component.close, 'emit');
    const compiled = fixture.nativeElement as HTMLElement;
    const backdrop = compiled.querySelector('.composite-modal-backdrop') as HTMLElement;

    backdrop.click();
    expect(closeSpy).toHaveBeenCalled();
  });

  it('should render image with composite-image class maintaining aspect ratio and alt text', () => {
    fixture.componentRef.setInput('compositeUrl', 'https://example.com/composite.jpg');
    fixture.componentRef.setInput('modelName', 'Adriana Lima');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const img = compiled.querySelector('.composite-image') as HTMLImageElement;
    expect(img).toBeTruthy();
    expect(img.src).toContain('https://example.com/composite.jpg');
    expect(img.alt).toContain('Adriana Lima');
  });

  it('should support zoom controls and download action', () => {
    fixture.componentRef.setInput('compositeUrl', 'https://example.com/composite.jpg');
    fixture.componentRef.setInput('modelName', 'Adriana Lima');
    fixture.detectChanges();

    expect(component.zoomLevel()).toBe(1);

    component.zoomIn();
    expect(component.zoomLevel()).toBe(1.25);

    component.zoomOut();
    expect(component.zoomLevel()).toBe(1);

    component.resetZoom();
    expect(component.zoomLevel()).toBe(1);

    // Download action
    component.downloadComposite();
    expect(component.isDownloading()).toBeTrue();

    const req = httpTesting.expectOne('https://example.com/composite.jpg');
    expect(req.request.method).toBe('GET');
    req.flush(new Blob(['fake-image-bytes'], { type: 'image/jpeg' }));

    expect(component.isDownloading()).toBeFalse();
  });
});
