import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'filterByCategory',
  standalone: true
})
export class FilterByCategoryPipe implements PipeTransform {
  transform<T extends { category?: string; gender?: string }>(items: T[] | null | undefined, filter: string): T[] {
    if (!items || !filter || filter === 'ALL') {
      return items || [];
    }

    const normalized = filter.toUpperCase();
    return items.filter(item =>
      (item.category && item.category.toUpperCase() === normalized) ||
      (item.gender && item.gender.toUpperCase() === normalized)
    );
  }
}
