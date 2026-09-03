import type { KeyboardEvent } from 'react';

const focusableSelector =
  'button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [href], [tabindex]:not([tabindex="-1"])';

export const focusFirstControl = (root: HTMLElement | null): void => {
  root?.querySelector<HTMLElement>(focusableSelector)?.focus();
};

/** NFR-004: keep keyboard focus inside a modal and provide one Escape close path. */
export const handleDialogKeyDown = (
  event: KeyboardEvent<HTMLElement>,
  onEscape: () => void,
): void => {
  if (event.key === 'Escape') {
    event.preventDefault();
    onEscape();
    return;
  }
  if (event.key !== 'Tab') return;

  const controls = Array.from(event.currentTarget.querySelectorAll<HTMLElement>(focusableSelector));
  const first = controls.at(0);
  const last = controls.at(-1);
  if (first === undefined || last === undefined) return;
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
};
