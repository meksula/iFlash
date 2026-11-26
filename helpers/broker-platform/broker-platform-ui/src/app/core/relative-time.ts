import { Pipe, PipeTransform } from '@angular/core';
import { formatDate } from '@angular/common';

const UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['day', 86400],
  ['hour', 3600],
  ['minute', 60],
];

const FORMAT = new Intl.RelativeTimeFormat('en', { numeric: 'auto' });

@Pipe({ name: 'relativeTime' })
export class RelativeTimePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }
    const seconds = Math.round((new Date(value).getTime() - Date.now()) / 1000);
    if (Math.abs(seconds) >= 7 * 86400) {
      return formatDate(value, 'd MMM y, HH:mm', 'en-US');
    }
    for (const [unit, size] of UNITS) {
      if (Math.abs(seconds) >= size) {
        return FORMAT.format(Math.trunc(seconds / size), unit);
      }
    }
    return 'just now';
  }
}
