export function isBackdropPress(event: MouseEvent, dialog: HTMLDialogElement): boolean {
  const box = dialog.getBoundingClientRect();
  const outside =
    event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom;
  return event.target === dialog && outside;
}
