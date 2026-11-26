import { Component, ElementRef, computed, inject, output, signal, viewChild } from '@angular/core';
import { Api } from '../../core/api';
import { Auth } from '../../core/auth';
import { Toast, fillMessage } from '../../core/toast';
import { UsdPipe } from '../../core/usd';
import { isBackdropPress } from './dialog-backdrop';

@Component({
  selector: 'app-order-dialog',
  imports: [UsdPipe],
  templateUrl: './order-dialog.html',
  styleUrl: './order-dialog.scss',
})
export class OrderDialog {
  private api = inject(Api);
  protected auth = inject(Auth);
  private toast = inject(Toast);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  readonly placed = output<void>();

  readonly ticker = signal('');
  readonly price = signal<number | null>(null);
  readonly position = signal(0);
  readonly direction = signal('BID');
  readonly orderType = signal('MARKET');
  readonly orderPrice = signal<number | null>(null);
  readonly volume = signal<number | null>(null);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  readonly isMarket = computed(() => this.orderType() === 'MARKET');
  readonly estimate = computed(() => {
    const unit = this.isMarket() ? this.price() : this.orderPrice();
    const volume = this.volume();
    return unit !== null && volume ? unit * volume : null;
  });

  open(ticker: string, direction: string): void {
    this.ticker.set(ticker);
    this.direction.set(direction);
    this.orderType.set('MARKET');
    this.orderPrice.set(null);
    this.volume.set(1);
    this.message.set(null);
    this.error.set(null);
    this.api.instrument(ticker).subscribe({
      next: (detail) => {
        this.price.set(detail.price);
        this.position.set(detail.position);
      },
    });
    this.dialog().nativeElement.showModal();
  }

  close(): void {
    this.dialog().nativeElement.close();
  }

  closeOnBackdrop(event: MouseEvent): void {
    if (isBackdropPress(event, this.dialog().nativeElement)) {
      this.close();
    }
  }

  submit(event: Event): void {
    event.preventDefault();
    const volume = this.volume();
    if (!volume || volume <= 0 || this.busy()) {
      return;
    }
    if (!this.isMarket() && (!this.orderPrice() || this.orderPrice()! <= 0)) {
      this.error.set('Price is required for a ' + this.orderType() + ' order.');
      return;
    }
    this.busy.set(true);
    this.message.set(null);
    this.error.set(null);
    this.api
      .placeOrder({
        direction: this.direction(),
        orderType: this.orderType(),
        ticker: this.ticker(),
        price: this.isMarket() ? null : this.orderPrice(),
        volume,
      })
      .subscribe({
        next: (res) => {
          this.volume.set(1);
          this.busy.set(false);
          this.placed.emit();
          if (res.outcome.filledQuantity > 0) {
            this.close();
            this.toast.show(fillMessage(res.outcome));
          } else {
            this.message.set(res.message);
          }
        },
        error: (err) => {
          this.error.set(err?.error?.error ?? 'Order failed.');
          this.busy.set(false);
        },
      });
  }
}
