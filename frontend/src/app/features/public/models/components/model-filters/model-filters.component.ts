import { Component, OnInit, OnDestroy, input, output, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { TranslatePipe } from '../../../../../shared/pipes/translate.pipe';

export interface FilterChangeEvent {
  search: string;
  sort: string;
}

@Component({
  selector: 'app-model-filters',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe],
  templateUrl: './model-filters.component.html',
  styleUrls: ['./model-filters.component.scss']
})
export class ModelFiltersComponent implements OnInit, OnDestroy {
  readonly totalCount = input<number>(0);
  readonly initialSearch = input<string>('');
  readonly initialSort = input<string>('stageName,asc');

  readonly filterChange = output<FilterChangeEvent>();
  readonly resetFilters = output<void>();

  readonly searchControl = new FormControl<string>('', { nonNullable: true });
  readonly sortControl = new FormControl<string>('stageName,asc', { nonNullable: true });

  private searchSub?: Subscription;
  private sortSub?: Subscription;

  constructor() {
    // Sincroniza inputs iniciais com os FormControls
    effect(() => {
      const searchVal = this.initialSearch();
      if (this.searchControl.value !== searchVal) {
        this.searchControl.setValue(searchVal, { emitEvent: false });
      }
    });

    effect(() => {
      const sortVal = this.initialSort();
      if (this.sortControl.value !== sortVal) {
        this.sortControl.setValue(sortVal, { emitEvent: false });
      }
    });
  }

  ngOnInit(): void {
    // Debounce de 300ms na pesquisa textual para evitar requisições redundantes
    this.searchSub = this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe((search) => {
        this.filterChange.emit({
          search: search.trim(),
          sort: this.sortControl.value
        });
      });

    // Mudança imediata de ordenação
    this.sortSub = this.sortControl.valueChanges
      .pipe(distinctUntilChanged())
      .subscribe((sort) => {
        this.filterChange.emit({
          search: this.searchControl.value.trim(),
          sort
        });
      });
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
    this.sortSub?.unsubscribe();
  }

  hasActiveFilters(): boolean {
    return this.searchControl.value.trim().length > 0 || this.sortControl.value !== 'stageName,asc';
  }

  onClearFilters(): void {
    this.searchControl.setValue('', { emitEvent: false });
    this.sortControl.setValue('stageName,asc', { emitEvent: false });
    this.resetFilters.emit();
  }
}
