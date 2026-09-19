import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ModelFiltersComponent } from './model-filters.component';
import { TranslationService } from '../../../../../core/services/translation.service';

describe('ModelFiltersComponent', () => {
  let component: ModelFiltersComponent;
  let fixture: ComponentFixture<ModelFiltersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModelFiltersComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        TranslationService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ModelFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create filters component', () => {
    expect(component).toBeTruthy();
    expect(component.searchControl.value).toBe('');
    expect(component.sortControl.value).toBe('stageName,asc');
  });

  it('should emit filterChange after 300ms debounce when search changes', fakeAsync(() => {
    let emittedEvent: any = null;
    component.filterChange.subscribe(event => {
      emittedEvent = event;
    });

    component.searchControl.setValue('Isabella');
    expect(emittedEvent).toBeNull(); // Não emite imediatamente

    tick(150);
    expect(emittedEvent).toBeNull(); // Ainda não completou 300ms

    tick(160);
    expect(emittedEvent).toEqual({
      search: 'Isabella',
      sort: 'stageName,asc'
    });
  }));

  it('should emit filterChange immediately when sort changes', () => {
    let emittedEvent: any = null;
    component.filterChange.subscribe(event => {
      emittedEvent = event;
    });

    component.sortControl.setValue('createdAt,desc');
    expect(emittedEvent).toEqual({
      search: '',
      sort: 'createdAt,desc'
    });
  });

  it('should reset filters and emit resetFilters on clear', () => {
    spyOn(component.resetFilters, 'emit');

    component.searchControl.setValue('Busca');
    component.sortControl.setValue('stageName,desc');
    expect(component.hasActiveFilters()).toBeTrue();

    component.onClearFilters();
    expect(component.searchControl.value).toBe('');
    expect(component.sortControl.value).toBe('stageName,asc');
    expect(component.resetFilters.emit).toHaveBeenCalled();
  });
});
