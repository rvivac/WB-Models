import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'measurement',
  standalone: true
})
export class MeasurementPipe implements PipeTransform {
  transform(value: number | undefined | null, unit: 'cm' | 'kg' | 'm' = 'cm'): string {
    if (value === undefined || value === null || isNaN(value)) {
      return '-';
    }

    if (unit === 'm') {
      return (value / 100).toFixed(2) + ' m';
    }

    return `${value} ${unit}`;
  }
}
