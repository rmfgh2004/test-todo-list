import { useState } from 'react';
import { addDays, weekRangeFor } from '@/shared/time/calendar';
import { deriveWeekCapacity } from '@/shared/grid/capacity';
import { BacklogPanel } from '@/features/backlog/BacklogPanel';
import { ConflictDialog } from '@/features/conflict-resolution/ConflictDialog';
import {
  DeleteConfirmDialog,
  TaskEditorDialog,
  type TaskContentModel,
} from '@/features/task-editor/components';
import { useTaskCommands } from '@/features/task-editor/use-task-commands';
import { TaskListPage } from '@/features/task-list/TaskListPage';
import { useTaskList } from '@/features/task-list/use-task-list';
import {
  CapacityIndicator,
  TimetableGrid,
  WeekNavigator,
  type WeekDirection,
} from '@/features/timetable/components';
import {
  SchedulingInteraction,
  type SlotProposal,
} from '@/features/timetable/SchedulingInteraction';
import { useScheduleCommands } from '@/features/timetable/use-schedule-commands';
import { SchedulingDndContext } from '@/features/timetable/dnd';
import { useWeeklyPlan } from '@/features/timetable/use-weekly-plan';
import { parseTaskListQuery, serializeTaskListQuery } from '@/shared/api/query';
import { ErrorStatusSurface, LoadingSurface } from '@/shared/ui/status-surface';
import styles from './App.module.css';

const seoulToday = (): string => {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const value = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((part) => part.type === type)?.value ?? '';
  return `${value('year')}-${value('month')}-${value('day')}`;
};

const safeError = (error: unknown): { message: string; requestId: string } => {
  if (
    typeof error === 'object' &&
    error !== null &&
    'message' in error &&
    typeof error.message === 'string' &&
    'requestId' in error &&
    typeof error.requestId === 'string'
  ) {
    return { message: error.message, requestId: error.requestId };
  }
  return { message: '계획을 불러오지 못했습니다.', requestId: 'TMP-UNKNOWN-0000' };
};

const weekLabel = (weekStart: string): string => {
  const end = addDays(weekStart, 6);
  return `${weekStart.replaceAll('-', '.')} – ${end.replaceAll('-', '.')}`;
};

export function LiveWorkspace({
  createOpen,
  onCreateClose,
}: {
  readonly createOpen: boolean;
  readonly onCreateClose: () => void;
}) {
  if (window.location.pathname === '/tasks') return <LiveTaskList />;
  return <LiveWeeklyWorkspace createOpen={createOpen} onCreateClose={onCreateClose} />;
}

function LiveWeeklyWorkspace({
  createOpen,
  onCreateClose,
}: {
  readonly createOpen: boolean;
  readonly onCreateClose: () => void;
}) {
  const [weekStart, setWeekStart] = useState(() => weekRangeFor(seoulToday()).start);
  const [selectedId, setSelectedId] = useState<string>();
  const [schedulingId, setSchedulingId] = useState<string>();
  const [draggingId, setDraggingId] = useState<string>();
  const [preview, setPreview] = useState<{ readonly date: string; readonly startTime: string }>();
  const [deleteId, setDeleteId] = useState<string>();
  const [invoker, setInvoker] = useState<HTMLElement>(() => document.body);
  const [loadTimedOut, setLoadTimedOut] = useState(false);
  const week = useWeeklyPlan(weekStart);
  const commands = useTaskCommands(weekStart);
  const scheduling = useScheduleCommands();

  if (week.isPending && loadTimedOut)
    return (
      <main id="main-content">
        <ErrorStatusSurface
          message="계획을 불러오는 데 시간이 오래 걸립니다."
          requestId="TMP-WEEK-TIME-0001"
          onRetry={() => {
            setLoadTimedOut(false);
            void week.refetch();
          }}
        />
      </main>
    );
  if (week.isPending)
    return (
      <main className={styles.workspace} id="main-content" aria-busy="true">
        <aside className={styles.backlog}>
          <h2>미배치 할 일</h2>
          <LoadingSurface onTimeout={() => setLoadTimedOut(true)} />
        </aside>
        <section className={styles.planner} aria-label="주간 계획 불러오는 중">
          <div className={styles.weekHeader}>
            <WeekNavigator weekLabel={weekLabel(weekStart)} onSelect={() => undefined} />
            <CapacityIndicator windowMinutes={5_880} plannedMinutes={0} availableMinutes={5_880} />
          </div>
          <TimetableGrid
            weekStart={weekStart}
            blocks={[]}
            onOpenTask={() => undefined}
            onSlotProposal={() => undefined}
          />
        </section>
      </main>
    );
  if (week.isError || week.data === undefined) {
    const error = safeError(week.error);
    return (
      <main id="main-content">
        <ErrorStatusSurface {...error} onRetry={() => void week.refetch()} />
      </main>
    );
  }

  const allTasks = [...week.data.scheduled, ...week.data.backlog];
  const selected = allTasks.find((task) => task.id === selectedId);
  const schedulingTask = allTasks.find((task) => task.id === schedulingId);
  const capacity = deriveWeekCapacity(week.data.scheduled);
  const selectWeek = (direction: WeekDirection) => {
    setLoadTimedOut(false);
    setWeekStart((current) =>
      direction === 'today'
        ? weekRangeFor(seoulToday()).start
        : addDays(current, direction === 'previous' ? -7 : 7),
    );
  };
  const beginSchedule = (taskId: string) => {
    if (document.activeElement instanceof HTMLElement) setInvoker(document.activeElement);
    setSchedulingId(taskId);
  };
  const propose = (proposal: SlotProposal) => {
    if (schedulingTask === undefined) return;
    scheduling.schedule.mutate({
      task: schedulingTask,
      date: proposal.date,
      startTime: proposal.startTime,
    });
  };
  const conflict = scheduling.schedule.error?.conflict;

  return (
    <>
      <SchedulingDndContext
        onDragStart={setDraggingId}
        onPreview={setPreview}
        onCancel={() => {
          setDraggingId(undefined);
          setPreview(undefined);
        }}
        onDrop={(taskId, proposal) => {
          const task = allTasks.find((item) => item.id === taskId);
          if (task !== undefined) scheduling.schedule.mutate({ task, ...proposal });
          setDraggingId(undefined);
          setPreview(undefined);
        }}
      >
        <main className={styles.workspace} id="main-content">
          <aside className={styles.backlog}>
            <BacklogPanel
              tasks={week.data.backlog}
              onOpenTask={setSelectedId}
              onSchedule={beginSchedule}
              onDragStart={setDraggingId}
            />
            {schedulingTask === undefined ? null : (
              <SchedulingInteraction
                taskId={schedulingTask.id}
                taskTitle={schedulingTask.title}
                estimateMinutes={schedulingTask.estimateMinutes}
                expectedVersion={schedulingTask.version}
                initialDate={weekStart}
                initialTime="09:00"
                onProposal={propose}
              />
            )}
          </aside>
          <section className={styles.planner} aria-label="주간 계획">
            <div className={styles.weekHeader}>
              <WeekNavigator weekLabel={weekLabel(weekStart)} onSelect={selectWeek} />
              <CapacityIndicator
                {...capacity}
                {...(preview === undefined || draggingId === undefined
                  ? {}
                  : {
                      previewDeltaMinutes: -(
                        allTasks.find((task) => task.id === draggingId)?.estimateMinutes ?? 0
                      ),
                    })}
              />
            </div>
            <TimetableGrid
              weekStart={weekStart}
              blocks={week.data.scheduled.flatMap((task) =>
                task.schedule === undefined
                  ? []
                  : [{ id: task.id, title: task.title, priority: task.priority, ...task.schedule }],
              )}
              onOpenTask={setSelectedId}
              onPreviewChange={setPreview}
              {...(scheduling.revertedTaskId === undefined
                ? {}
                : { revertedTaskId: scheduling.revertedTaskId })}
              onSlotProposal={(proposal) => {
                const task = allTasks.find((item) => item.id === draggingId);
                if (task === undefined) return;
                scheduling.schedule.mutate({ task, ...proposal });
                setDraggingId(undefined);
              }}
            />
          </section>
        </main>
      </SchedulingDndContext>
      {createOpen ? (
        <TaskEditorDialog
          mode="create"
          onClose={onCreateClose}
          onSubmit={async (content) => {
            await commands.create.mutateAsync(content);
            onCreateClose();
          }}
        />
      ) : null}
      {selected === undefined ? null : (
        <TaskEditorDialog
          mode="update"
          initial={{
            title: selected.title,
            description: selected.description ?? null,
            priority: selected.priority,
            estimateMinutes: selected.estimateMinutes,
            dueDate: selected.dueDate ?? null,
          }}
          onClose={() => setSelectedId(undefined)}
          onSubmit={async (content: TaskContentModel) => {
            await commands.update.mutateAsync({ task: selected, content });
            setSelectedId(undefined);
          }}
          onRequestDelete={() => {
            if (document.activeElement instanceof HTMLElement) setInvoker(document.activeElement);
            setDeleteId(selected.id);
          }}
          {...(selected.schedule === undefined
            ? {}
            : {
                onUnschedule: async () => {
                  await scheduling.unschedule.mutateAsync(selected);
                  setSelectedId(undefined);
                },
              })}
        />
      )}
      {deleteId === undefined || selected === undefined ? null : (
        <DeleteConfirmDialog
          taskTitle={selected.title}
          invoker={invoker}
          onCancel={() => setDeleteId(undefined)}
          onConfirm={() => {
            void commands.remove.mutateAsync(selected).then(() => {
              setDeleteId(undefined);
              setSelectedId(undefined);
            });
          }}
        />
      )}
      {conflict === undefined || schedulingTask === undefined ? null : (
        <ConflictDialog
          taskTitle={schedulingTask.title}
          proposed={conflict.proposed}
          conflicting={conflict.conflicting}
          {...(conflict.nextCandidate === undefined
            ? {}
            : { nextCandidate: conflict.nextCandidate })}
          invoker={invoker}
          onCancel={() => scheduling.schedule.reset()}
          onChooseCandidate={(date, startTime) =>
            scheduling.schedule.mutate({
              task: schedulingTask,
              date,
              startTime,
              resolutionMode: 'ACCEPT_CANDIDATE',
            })
          }
        />
      )}
    </>
  );
}

function LiveTaskList() {
  const [search, setSearch] = useState(() => window.location.search);
  const query = serializeTaskListQuery(parseTaskListQuery(new URLSearchParams(search)));
  const tasks = useTaskList(query);
  const commands = useTaskCommands(weekRangeFor(seoulToday()).start);
  if (tasks.isPending)
    return (
      <main id="main-content">
        <LoadingSurface />
      </main>
    );
  if (tasks.isError || tasks.data === undefined) {
    const error = safeError(tasks.error);
    return (
      <main id="main-content">
        <ErrorStatusSurface {...error} onRetry={() => void tasks.refetch()} />
      </main>
    );
  }
  return (
    <main className={styles.listWorkspace} id="main-content">
      <TaskListPage
        tasks={tasks.data.content}
        page={tasks.data.page}
        totalPages={tasks.data.totalPages}
        filtered={new URLSearchParams(search).has('status')}
        onSearchChange={setSearch}
        onOpenTask={() => undefined}
        onToggleCompletion={(taskId, completed) => {
          const task = tasks.data.content.find((item) => item.id === taskId);
          if (task !== undefined) commands.completion.mutate({ task, completed });
        }}
      />
    </main>
  );
}
