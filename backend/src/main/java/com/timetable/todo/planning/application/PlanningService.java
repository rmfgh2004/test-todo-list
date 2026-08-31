package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.DomainValidationException;
import com.timetable.todo.planning.domain.EstimateMinutes;
import com.timetable.todo.planning.domain.SchedulePolicy;
import com.timetable.todo.planning.domain.ScheduleSlot;
import com.timetable.todo.planning.domain.Task;
import com.timetable.todo.planning.domain.TaskId;
import com.timetable.todo.planning.domain.WeekRange;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

/** FR-001~FR-010, FR-013: Coordinates authoritative planning commands and queries. */
public class PlanningService {

  private static final String LOCAL_ACTOR = "local-user";
  private static final int BACKLOG_LIMIT = 100;

  private final TaskRepositoryPort tasks;
  private final AuditPort audits;
  private final Clock clock;
  private final Supplier<UUID> idGenerator;

  public PlanningService(
      TaskRepositoryPort tasks, AuditPort audits, Clock clock, Supplier<UUID> idGenerator) {
    this.tasks = Objects.requireNonNull(tasks);
    this.audits = Objects.requireNonNull(audits);
    this.clock = Objects.requireNonNull(clock);
    this.idGenerator = Objects.requireNonNull(idGenerator);
  }

  @Transactional
  public Task create(CreateTaskCommand command, String requestId) {
    Instant now = clock.instant();
    Task created =
        Task.create(
            new TaskId(idGenerator.get()),
            command.title(),
            command.description(),
            command.priority(),
            command.estimateMinutes(),
            command.dueDate(),
            now);
    Task saved = tasks.save(created);
    appendAudit(saved.id(), AuditAction.CREATED, requestId, now, List.of("task"));
    return saved;
  }

  /**
   * FR-004, FR-007: Replaces the full content set and re-validates an existing placement.
   *
   * <p>A new estimate keeps the placement start and recalculates its end. The resized interval is
   * re-checked against the current week, so an overlap returns a conflict and writes nothing.
   */
  @Transactional
  public ScheduleOutcome update(UpdateTaskCommand command, String requestId) {
    Task task = current(command.taskId(), command.expectedVersion());
    Instant now = clock.instant();
    Task updated =
        task.update(
            command.title(),
            command.description(),
            command.priority(),
            command.estimateMinutes(),
            command.dueDate(),
            now);

    Optional<ScheduleSlot> resized = updated.schedule();
    if (resized.isPresent() && !resized.equals(task.schedule())) {
      Optional<ScheduleOutcome> conflict = detectConflict(task.id(), resized.get());
      if (conflict.isPresent()) {
        return conflict.get();
      }
    }
    Task saved = tasks.save(updated);
    appendAudit(saved.id(), AuditAction.UPDATED, requestId, now, List.of("content"));
    return new ScheduleOutcome.Committed(saved);
  }

  @Transactional
  public ScheduleOutcome schedule(ScheduleTaskCommand command, String requestId) {
    Task task = current(command.taskId(), command.expectedVersion());
    ScheduleSlot proposed = ScheduleSlot.of(command.date(), command.startTime(), task.estimate());

    Optional<ScheduleOutcome> conflict = detectConflict(task.id(), proposed);
    if (conflict.isPresent()) {
      return conflict.get();
    }

    boolean moving = task.schedule().isPresent();
    Instant now = clock.instant();
    Task saved = tasks.save(task.schedule(proposed, now));
    appendAudit(
        saved.id(),
        moving ? AuditAction.MOVED : AuditAction.SCHEDULED,
        requestId,
        now,
        List.of("schedule"));
    return new ScheduleOutcome.Committed(saved);
  }

  /** FR-008: Removes only the placement and stays idempotent for an unscheduled task. */
  @Transactional
  public Task unschedule(UnscheduleTaskCommand command, String requestId) {
    Task task = current(command.taskId(), command.expectedVersion());
    Instant now = clock.instant();
    Task changed = task.unschedule(now);
    if (changed == task) {
      return task;
    }
    Task saved = tasks.save(changed);
    appendAudit(saved.id(), AuditAction.UNSCHEDULED, requestId, now, List.of("schedule"));
    return saved;
  }

  @Transactional
  public Task setCompletion(SetCompletionCommand command, String requestId) {
    Task task = current(command.taskId(), command.expectedVersion());
    Instant now = clock.instant();
    Task changed = task.setCompleted(command.completed(), now);
    if (changed == task) {
      return task;
    }
    Task saved = tasks.save(changed);
    appendAudit(
        saved.id(),
        command.completed() ? AuditAction.COMPLETED : AuditAction.REOPENED,
        requestId,
        now,
        List.of("status"));
    return saved;
  }

  @Transactional
  public void delete(DeleteTaskCommand command, String requestId) {
    if (!command.confirmed()) {
      throw new DeletionNotConfirmedException();
    }
    Task task = current(command.taskId(), command.expectedVersion());
    Instant now = clock.instant();
    tasks.delete(task.id());
    appendAudit(task.id(), AuditAction.DELETED, requestId, now, List.of("task"));
  }

  @Transactional(readOnly = true)
  public Task findById(TaskId id) {
    return tasks.findById(id).orElseThrow(TaskNotFoundException::new);
  }

  /** FR-001, FR-005: Returns the bounded week placement view plus the current backlog. */
  @Transactional(readOnly = true)
  public WeeklyPlan weekPlan(WeekRange week) {
    return new WeeklyPlan(week, tasks.findScheduledInWeek(week), tasks.findBacklog(BACKLOG_LIMIT));
  }

  @Transactional(readOnly = true)
  public List<Task> week(WeekRange week) {
    return List.copyOf(tasks.findScheduledIncomplete(week));
  }

  @Transactional(readOnly = true)
  public List<Task> backlog(int limit) {
    if (limit < 1 || limit > 100) {
      throw new DomainValidationException(
          "BACKLOG_LIMIT_INVALID", "Backlog limit must be between 1 and 100");
    }
    return List.copyOf(tasks.findBacklog(limit));
  }

  @Transactional(readOnly = true)
  public TaskPage list(TaskListQuery query) {
    return tasks.findPage(query);
  }

  /**
   * FR-007: Re-evaluates the half-open overlap rule inside the mutation transaction.
   *
   * <p>The moving task never blocks itself and completed placements never block a new interval.
   */
  private Optional<ScheduleOutcome> detectConflict(TaskId movingTaskId, ScheduleSlot proposed) {
    WeekRange week =
        WeekRange.fromMonday(
            proposed.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    List<ScheduleSlot> occupied =
        tasks.findScheduledIncomplete(week).stream()
            .filter(existing -> !existing.id().equals(movingTaskId))
            .flatMap(existing -> existing.schedule().stream())
            .toList();
    EstimateMinutes proposedLength = EstimateMinutes.of(proposed.durationMinutes());

    return occupied.stream()
        .filter(slot -> SchedulePolicy.overlaps(proposed, slot))
        .findFirst()
        .map(
            conflicting ->
                new ScheduleOutcome.Conflict(
                    proposed,
                    conflicting,
                    SchedulePolicy.findNextAvailable(
                        week, proposed.date(), proposed.start(), proposedLength, occupied)));
  }

  private Task current(TaskId id, long expectedVersion) {
    Task task = tasks.findById(id).orElseThrow(TaskNotFoundException::new);
    if (task.version() != expectedVersion) {
      throw new StaleTaskVersionException(task.version());
    }
    return task;
  }

  private void appendAudit(
      TaskId taskId,
      AuditAction action,
      String requestId,
      Instant occurredAt,
      List<String> fields) {
    audits.append(
        new AuditEvent(
            UUID.randomUUID(), taskId, action, LOCAL_ACTOR, requestId, occurredAt, fields));
  }
}
