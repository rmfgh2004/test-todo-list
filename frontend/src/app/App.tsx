import { ChevronLeft, ChevronRight, Clock3, Moon, Plus, Sun, WifiOff } from 'lucide-react';
import { useState, useSyncExternalStore } from 'react';
import type { ConnectivityState } from '@/shared/api/connectivity';
import { connectivityRuntime } from '@/shared/api/connectivity';
import { FeedbackProvider } from '@/shared/ui/feedback';
import { RouteErrorBoundary } from '@/shared/ui/route-error-boundary';
import '@/shared/ui/tokens.css';
import { useTheme } from '@/shared/ui/use-theme';
import { LiveWorkspace } from './LiveWorkspace';
import styles from './App.module.css';

const days = [
  ['월', '31'],
  ['화', '1'],
  ['수', '2'],
  ['목', '3'],
  ['금', '4'],
  ['토', '5'],
  ['일', '6'],
] as const;

const hours = Array.from({ length: 14 }, (_, index) => `${String(index + 8).padStart(2, '0')}:00`);

export interface AppProps {
  readonly connectivity?: ConnectivityState;
  readonly live?: boolean;
}

function WeekShell({
  connectivity,
  live,
}: {
  readonly connectivity: ConnectivityState;
  readonly live: boolean;
}) {
  const mutationsDisabled = connectivity === 'disconnected';
  const [createOpen, setCreateOpen] = useState(false);
  const taskListActive = live && window.location.pathname === '/tasks';
  return (
    <div className={styles.page}>
      <a className={styles.skipLink} href="#main-content">
        본문으로 건너뛰기
      </a>
      <header className={styles.header}>
        <div className={styles.brandGroup}>
          <a className={styles.brand} href="/" aria-label="Tempo 홈">
            Tempo
          </a>
          <nav className={styles.viewTabs} aria-label="보기 전환">
            <a
              className={taskListActive ? undefined : styles.activeTab}
              href="/"
              aria-current={taskListActive ? undefined : 'page'}
            >
              주간
            </a>
            <a
              className={taskListActive ? styles.activeTab : undefined}
              href="/tasks"
              aria-current={taskListActive ? 'page' : undefined}
            >
              목록
            </a>
          </nav>
        </div>
        <WeekControls
          {...(live ? { onCreate: () => setCreateOpen(true) } : {})}
          mutationsDisabled={mutationsDisabled}
        />
      </header>
      {connectivity === 'disconnected' ? (
        <div className={styles.connectivity} role="status">
          <WifiOff aria-hidden="true" size={16} />
          백엔드 연결을 확인하고 있습니다. 연결되면 조회를 자동으로 새로고침합니다.
        </div>
      ) : null}
      {live ? (
        <RouteErrorBoundary requestId="TMP-SHELL-INIT-0001">
          <LiveWorkspace createOpen={createOpen} onCreateClose={() => setCreateOpen(false)} />
        </RouteErrorBoundary>
      ) : (
        <RouteErrorBoundary requestId="TMP-SHELL-INIT-0001">
          <main className={styles.workspace} id="main-content">
            <aside className={styles.backlog} aria-labelledby="backlog-title">
              <div className={styles.panelHeading}>
                <div>
                  <p className={styles.eyebrow}>이번 주 계획</p>
                  <h1 id="backlog-title">미배치 할 일</h1>
                </div>
                <span className={styles.count}>0</span>
              </div>
              <div className={styles.emptyBacklog}>
                <div className={styles.emptyIcon} aria-hidden="true">
                  <Plus size={18} />
                </div>
                <strong>아직 할 일이 없습니다</strong>
                <p>할 일을 만들면 이곳에서 주간 그리드로 배치할 수 있어요.</p>
                <button type="button" disabled={mutationsDisabled}>
                  첫 할 일 만들기
                </button>
              </div>
              <section className={styles.hint} aria-label="시작하는 방법">
                <p className={styles.eyebrow}>시작하는 방법</p>
                <ol>
                  <li>할 일을 만들고 예상 시간을 정합니다.</li>
                  <li>카드를 끌거나 키보드로 시간을 선택합니다.</li>
                  <li>서버 확인 뒤 일정이 확정됩니다.</li>
                </ol>
              </section>
            </aside>
            <section className={styles.planner} aria-labelledby="week-title">
              <div className={styles.weekHeader}>
                <div className={styles.weekNav}>
                  <button type="button" aria-label="이전 주">
                    <ChevronLeft aria-hidden="true" size={18} />
                  </button>
                  <button type="button">오늘</button>
                  <button type="button" aria-label="다음 주">
                    <ChevronRight aria-hidden="true" size={18} />
                  </button>
                  <div>
                    <h2 id="week-title">2026년 8월 31일 – 9월 6일</h2>
                    <span>36주차</span>
                  </div>
                </div>
                <CapacityAndActions mutationsDisabled={mutationsDisabled} />
              </div>
              <div className={styles.gridFrame} aria-label="주간 시간표">
                <div className={styles.corner}>KST</div>
                {days.map(([label, date], index) => (
                  <div
                    className={`${styles.dayHeader} ${index > 4 ? styles.weekend : ''}`}
                    key={label}
                  >
                    <span>{label}</span>
                    <strong>{date}</strong>
                  </div>
                ))}
                <div className={styles.timeAxis}>
                  {hours.map((hour) => (
                    <span key={hour}>{hour}</span>
                  ))}
                </div>
                {days.map(([label], index) => (
                  <div
                    className={`${styles.dayColumn} ${index > 4 ? styles.weekend : ''}`}
                    key={label}
                  />
                ))}
                <div className={styles.emptyWeek}>
                  <Clock3 aria-hidden="true" size={24} />
                  <strong>이번 주는 아직 비어 있습니다</strong>
                  <span>왼쪽에서 할 일을 만든 뒤 원하는 시간에 배치해 보세요.</span>
                </div>
              </div>
            </section>
          </main>
        </RouteErrorBoundary>
      )}
    </div>
  );
}

function WeekControls({
  onCreate,
  mutationsDisabled,
}: {
  readonly onCreate?: () => void;
  readonly mutationsDisabled: boolean;
}) {
  const { theme, toggleTheme } = useTheme();
  return (
    <div className={styles.headerActions}>
      {onCreate === undefined ? null : (
        <button
          className={styles.primaryButton}
          type="button"
          disabled={mutationsDisabled}
          onClick={onCreate}
        >
          <Plus aria-hidden="true" size={17} />새 할 일
        </button>
      )}
      <button
        className={styles.iconButton}
        type="button"
        aria-label={theme === 'light' ? '다크 모드 사용' : '라이트 모드 사용'}
        onClick={toggleTheme}
      >
        {theme === 'light' ? (
          <Moon aria-hidden="true" size={17} />
        ) : (
          <Sun aria-hidden="true" size={17} />
        )}
      </button>
    </div>
  );
}

function CapacityAndActions({ mutationsDisabled }: { readonly mutationsDisabled: boolean }) {
  return (
    <div className={styles.planActions}>
      <div className={styles.capacity}>
        <Clock3 aria-hidden="true" size={16} />
        <span>이번 주 가용 시간</span>
        <strong>98h</strong>
      </div>
      <button className={styles.primaryButton} type="button" disabled={mutationsDisabled}>
        <Plus aria-hidden="true" size={17} />새 할 일
      </button>
    </div>
  );
}

/** F-C01: token-driven shell; feature routes arrive in Step 12. */
export function App({ connectivity, live = false }: AppProps) {
  const runtimeConnectivity = useSyncExternalStore(
    connectivityRuntime.subscribe,
    connectivityRuntime.getSnapshot,
    connectivityRuntime.getSnapshot,
  );
  return (
    <FeedbackProvider>
      <WeekShell
        connectivity={connectivity ?? (live ? runtimeConnectivity : 'connected')}
        live={live}
      />
    </FeedbackProvider>
  );
}
