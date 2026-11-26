import { Injectable, signal } from '@angular/core';
import { OrderOutcome } from './models';

@Injectable({ providedIn: 'root' })
export class Toast {
  readonly text = signal<string | null>(null);
  private timer?: ReturnType<typeof setTimeout>;

  show(text: string): void {
    clearTimeout(this.timer);
    this.text.set(text);
    this.timer = setTimeout(() => this.text.set(null), 2000);
  }
}

export function fillMessage(outcome: OrderOutcome): string {
  const side = outcome.direction === 'BID' ? 'Bought' : 'Sold';
  const partial =
    outcome.filledQuantity < outcome.requestedVolume ? ` (partial, of ${outcome.requestedVolume})` : '';
  return `${side} ${outcome.filledQuantity} ${outcome.ticker} for ${outcome.cashAmount.toFixed(2)} USD${partial}`;
}
