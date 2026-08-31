package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.DomainValidationException;
import com.timetable.todo.planning.domain.SchedulePolicy;
import com.timetable.todo.planning.domain.ScheduleSlot;
import com.timetable.todo.planning.domain.Task;
import com.timetable.todo.planning.domain.TaskId;
import com.timetable.todo.planning.domain.WeekRange;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

/** FR-001~FR-010, FR-013: Coordinates authoritative planning commands and queries. */
public class PlanningService {

  private static final String LOCAL_ACTOR = "local-user";
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

  @Transactional
  public ScheduleOutcome schedule(ScheduleTaskCommand command, String requestId) {
    Task task = current(command.taskId(), command.expectedVersion());
    ScheduleSlot proposed = ScheduleSlot.of(command.date(), command.startTime(), task.estimate());
    WeekRange week =
        WeekRange.fromMonday(
            command.date().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)));
    List<Task> scheduled = tasks.findScheduledIncomplete(week);
    List<ScheduleSlot> occupied =
        scheduled.stream()
            .filter(existing -> !existing.id().equals(task.id()))
            .flatMap(existing -> existing.schedule().stream())
            .toList();

    return occupied.stream()
        .filter(slot -> SchedulePolicy.overlaps(proposed, slot))
        .findFirst()
        .<ScheduleOutcome>map(
            conflicting ->
                new ScheduleOutcome.Conflict(
                    proposed,
                    conflicting,
                    SchedulePolicy.findNextAvailable(
                        week, command.date(), command.startTime(), task.estimate(), occupied)))
        .orElseGet(
            () -> {
              boolean moving = task.schedule().isPresent();
              Instant now = clock.instant();
              Task saved = tasks.save(task.schedule(proposed, now));
              appendAudit(
                  saved.id(),
                  moving ? AuditAction.MOVED : AuditAction.SCHEDULED,
                  requestId,
                  now,
                  List.of("schedule"));
              return new ScheduleOutcome.Scheduled(saved);
            });
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

  private Task current(TaskId id, long expectedVersion) {
    Task task = tasks.findById(id).orElseThrow(TaskNotFoundException::new);
    if (task.version() != expectedVersion) {
      throw new StaleTaskVersionException();
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
