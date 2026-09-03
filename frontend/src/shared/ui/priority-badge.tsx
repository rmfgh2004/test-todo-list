import styles from './priority-badge.module.css';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';

const presentation = {
  HIGH: { icon: '▲', label: '높음', className: styles.high },
  MEDIUM: { icon: '●', label: '보통', className: styles.medium },
  LOW: { icon: '▼', label: '낮음', className: styles.low },
} as const;

/** UR-016: priority is encoded by icon, text and colour together. */
export function PriorityBadge({ priority }: { readonly priority: Priority }) {
  const value = presentation[priority];
  return (
    <span className={`${styles.badge} ${value.className}`} data-priority={priority}>
      <span aria-hidden="true">{value.icon}</span>
      {value.label}
    </span>
  );
}
