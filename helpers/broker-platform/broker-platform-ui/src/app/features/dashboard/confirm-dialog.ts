import { Component, ElementRef, signal, viewChild } from '@angular/core';
import { isBackdropPress } from './dialog-backdrop';

@Component({
  selector: 'app-confirm-dialog',
  template: `
    <dialog #dialog class="confirm" (close)="settle()" (mousedown)="onMouseDown($event)">
      <p>{{ question() }}</p>
      <div class="confirm-actions">
        <button type="button" class="btn" (click)="answer(false)" autofocus>Cancel</button>
        <button type="button" class="btn confirm-yes" (click)="answer(true)">Yes</button>
      </div>
    </dialog>
  `,
  styles: `
    .confirm {
      width: min(360px, calc(100vw - 32px));
      padding: 16px;
      border: none;
      border-top: 3px solid var(--sell-fill);
      border-radius: 0;
      background: var(--surface);
      color: var(--ink);
      box-shadow: 0 18px 50px rgba(0, 0, 0, 0.5);
    }
    .confirm::backdrop { background: rgba(3, 7, 15, 0.65); }
    p { margin: 0 0 16px; font-size: 14px; font-weight: 700; }
    .confirm-actions { display: flex; justify-content: flex-end; gap: 8px; }
    .confirm-actions .btn { min-width: 84px; }
    .confirm-actions .confirm-yes { background: var(--sell-fill); border-color: var(--sell-fill); color: #fff; }
    .confirm-actions .confirm-yes:hover { filter: brightness(1.1); }
  `,
})
export class ConfirmDialog {
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private resolve?: (confirmed: boolean) => void;
  private confirmed = false;

  readonly question = signal('');

  ask(question: string): Promise<boolean> {
    this.question.set(question);
    this.confirmed = false;
    this.dialog().nativeElement.showModal();
    return new Promise((resolve) => (this.resolve = resolve));
  }

  answer(confirmed: boolean): void {
    this.confirmed = confirmed;
    this.dialog().nativeElement.close();
  }

  settle(): void {
    this.resolve?.(this.confirmed);
    this.resolve = undefined;
  }

  onMouseDown(event: MouseEvent): void {
    if (isBackdropPress(event, this.dialog().nativeElement)) {
      this.answer(false);
    }
  }
}
