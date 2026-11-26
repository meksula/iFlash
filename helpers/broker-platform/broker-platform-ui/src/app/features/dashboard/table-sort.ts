import { signal } from '@angular/core';

type SortValue = string | number | null;

export class TableSort<T> {
  readonly key = signal('');
  readonly asc = signal(true);

  constructor(
    private readonly columns: Record<string, (row: T) => SortValue>,
    initial: string,
  ) {
    this.key.set(initial);
  }

  by(key: string): void {
    if (this.key() === key) {
      this.asc.set(!this.asc());
    } else {
      this.key.set(key);
      this.asc.set(true);
    }
  }

  mark(key: string): string {
    return this.key() === key ? (this.asc() ? '▲' : '▼') : '';
  }

  apply(rows: T[]): T[] {
    const value = this.columns[this.key()];
    const direction = this.asc() ? 1 : -1;
    return [...rows].sort((a, b) => direction * compare(value(a), value(b)));
  }
}

function compare(a: SortValue, b: SortValue): number {
  if (a === b) {
    return 0;
  }
  if (a === null) {
    return 1;
  }
  if (b === null) {
    return -1;
  }
  return a < b ? -1 : 1;
}
