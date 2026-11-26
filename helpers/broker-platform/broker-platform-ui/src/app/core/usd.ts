import { Pipe, PipeTransform } from '@angular/core';
import { formatNumber } from '@angular/common';

@Pipe({ name: 'usd' })
export class UsdPipe implements PipeTransform {
  transform(value: number | null | undefined, signed = false): string {
    if (value === null || value === undefined) {
      return '—';
    }
    const sign = signed && value > 0 ? '+' : '';
    return `${sign}${formatNumber(value, 'en-US', '1.2-2')} USD`;
  }
}

@Pipe({ name: 'percent' })
export class PercentPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '—';
    }
    const sign = value > 0 ? '+' : '';
    return `${sign}${formatNumber(value, 'en-US', '1.2-2')}%`;
  }
}
