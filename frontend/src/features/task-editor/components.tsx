import { useEffect, useRef, useState, type FormEvent } from 'react';
import { focusFirstControl, handleDialogKeyDown } from '@/shared/ui/dialog-focus';
import styles from '@/shared/ui/feature.module.css';

export interface TaskContentModel {
  readonly title: string;
  readonly description: string | null;
  readonly priority: 'LOW' | 'MEDIUM' | 'HIGH';
  readonly estimateMinutes: number;
  readonly dueDate: string | null;
}

export interface TaskEditorDialogProps {
  readonly mode: 'create' | 'update';
  readonly initial?: TaskContentModel;
  readonly onClose: () => void;
  readonly onSubmit: (content: TaskContentModel) => void | Promise<void>;
  readonly onRequestDelete?: () => void;
  readonly onUnschedule?: () => void;
}

const empty: TaskContentModel = {
  title: '',
  description: null,
  priority: 'MEDIUM',
  estimateMinutes: 30,
  dueDate: null,
};

/** F-C04, UR-001~008: local defensive form validation; U1 remains authoritative. */
export function TaskEditorDialog({
  mode,
  initial = empty,
  onClose,
  onSubmit,
  onRequestDelete,
  onUnschedule,
}: TaskEditorDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const invokerRef = useRef<HTMLElement | null>(
    document.activeElement instanceof HTMLElement ? document.activeElement : null,
  );
  const titleRef = useRef<HTMLInputElement>(null);
  const [title, setTitle] = useState(initial.title);
  const [description, setDescription] = useState(initial.description ?? '');
  const [priority, setPriority] = useState(initial.priority);
  const [estimate, setEstimate] = useState(String(initial.estimateMinutes));
  const [dueDate, setDueDate] = useState(initial.dueDate ?? '');
  const [error, setError] = useState('');

  useEffect(() => titleRef.current?.focus(), []);

  const close = () => {
    onClose();
    invokerRef.current?.focus();
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const trimmed = title.trim();
    const estimateMinutes = Number(estimate);
    if (trimmed.length === 0) {
      setError('제목을 입력해 주세요');
      titleRef.current?.focus();
      return;
    }
    if (
      trimmed.length > 200 ||
      description.length > 5_000 ||
      !Number.isInteger(estimateMinutes) ||
      estimateMinutes < 15 ||
      estimateMinutes > 840 ||
      estimateMinutes % 15 !== 0
    ) {
      setError('입력값을 확인해 주세요');
      return;
    }
    setError('');
    void onSubmit({
      title: trimmed,
      description: description.trim() === '' ? null : description,
      priority,
      estimateMinutes,
      dueDate: dueDate === '' ? null : dueDate,
    });
  };

  return (
    <div className={styles.dialogBackdrop}>
      <dialog
        ref={dialogRef}
        open
        className={styles.dialog}
        aria-modal="true"
        aria-labelledby="task-editor-title"
        onKeyDown={(event) => handleDialogKeyDown(event, close)}
      >
        <h2 id="task-editor-title">{mode === 'create' ? '새 할 일' : '할 일 수정'}</h2>
        <form className={styles.form} onSubmit={submit}>
          <label>
            제목
            <input
              ref={titleRef}
              value={title}
              maxLength={200}
              onChange={(event) => setTitle(event.currentTarget.value)}
            />
          </label>
          <label>
            설명
            <textarea
              value={description}
              maxLength={5_000}
              onChange={(event) => setDescription(event.currentTarget.value)}
            />
          </label>
          <label>
            우선순위
            <select
              value={priority}
              onChange={(event) =>
                setPriority(
                  event.currentTarget.value === 'LOW'
                    ? 'LOW'
                    : event.currentTarget.value === 'HIGH'
                      ? 'HIGH'
                      : 'MEDIUM',
                )
              }
            >
              <option value="LOW">낮음</option>
              <option value="MEDIUM">보통</option>
              <option value="HIGH">높음</option>
            </select>
          </label>
          <label>
            예상 시간
            <input
              type="number"
              min="15"
              max="840"
              step="15"
              value={estimate}
              onChange={(event) => setEstimate(event.currentTarget.value)}
            />
          </label>
          <label>
            마감일
            <input
              type="date"
              value={dueDate}
              onChange={(event) => setDueDate(event.currentTarget.value)}
            />
          </label>
          {error === '' ? null : <p className={styles.errorText}>{error}</p>}
          <div className={styles.actions}>
            {onRequestDelete === undefined ? null : (
              <button className={styles.danger} type="button" onClick={onRequestDelete}>
                삭제
              </button>
            )}
            {onUnschedule === undefined ? null : (
              <button className={styles.secondary} type="button" onClick={onUnschedule}>
                시간 배치 해제
              </button>
            )}
            <button className={styles.secondary} type="button" onClick={close}>
              취소
            </button>
            <button className={styles.primary} type="submit">
              {mode === 'create' ? '만들기' : '저장'}
            </button>
          </div>
        </form>
      </dialog>
    </div>
  );
}

export function DeleteConfirmDialog({
  taskTitle,
  invoker,
  onCancel,
  onConfirm,
}: {
  readonly taskTitle: string;
  readonly invoker: HTMLElement;
  readonly onCancel: () => void;
  readonly onConfirm: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  useEffect(() => focusFirstControl(dialogRef.current), []);
  const cancel = () => {
    onCancel();
    invoker.focus();
  };
  return (
    <div className={styles.dialogBackdrop}>
      <dialog
        ref={dialogRef}
        open
        className={styles.dialog}
        aria-modal="true"
        aria-labelledby="delete-title"
        onKeyDown={(event) => handleDialogKeyDown(event, cancel)}
      >
        <h2 id="delete-title">할 일 삭제</h2>
        <p>“{taskTitle}”을 삭제할까요? 이 작업은 되돌릴 수 없습니다.</p>
        <div className={styles.actions}>
          <button className={styles.secondary} type="button" onClick={cancel}>
            취소
          </button>
          <button className={styles.danger} type="button" onClick={onConfirm}>
            삭제 확인
          </button>
        </div>
      </dialog>
    </div>
  );
}
