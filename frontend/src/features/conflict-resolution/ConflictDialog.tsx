import { useEffect, useRef } from 'react';
import { focusFirstControl, handleDialogKeyDown } from '@/shared/ui/dialog-focus';
import styles from '@/shared/ui/feature.module.css';

export interface ConflictSlotModel {
  readonly date: string;
  readonly startTime: string;
  readonly endTime: string;
}

export function ConflictDialog({
  taskTitle,
  proposed,
  conflicting,
  nextCandidate,
  invoker,
  onChooseCandidate,
  onCancel,
}: {
  readonly taskTitle: string;
  readonly proposed: ConflictSlotModel;
  readonly conflicting: ConflictSlotModel;
  readonly nextCandidate?: ConflictSlotModel;
  readonly invoker: HTMLElement;
  readonly onChooseCandidate: (date: string, startTime: string) => void;
  readonly onCancel: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => focusFirstControl(dialog.current), []);
  const cancel = () => {
    onCancel();
    invoker.focus();
  };
  return (
    <div className={styles.dialogBackdrop}>
      <dialog
        open
        className={styles.dialog}
        aria-modal="true"
        aria-labelledby="conflict-title"
        tabIndex={-1}
        ref={dialog}
        onKeyDown={(event) => handleDialogKeyDown(event, cancel)}
      >
        <h2 id="conflict-title">{taskTitle} 배치 충돌</h2>
        <div className={styles.comparison}>
          <div>
            <strong>선택한 시간</strong>
            <p>
              {proposed.startTime}–{proposed.endTime}
            </p>
          </div>
          <div>
            <strong>이미 사용 중</strong>
            <p>
              {conflicting.startTime}–{conflicting.endTime}
            </p>
          </div>
        </div>
        <div className={styles.actions}>
          <button className={styles.secondary} type="button" onClick={cancel}>
            기존 일정 유지
          </button>
          {nextCandidate === undefined ? null : (
            <button
              className={styles.primary}
              type="button"
              onClick={() => onChooseCandidate(nextCandidate.date, nextCandidate.startTime)}
            >
              {nextCandidate.startTime}으로 이동
            </button>
          )}
        </div>
      </dialog>
    </div>
  );
}
