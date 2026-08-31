package com.timetable.todo.planning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.timetable.todo.planning.domain.Priority;
import com.timetable.todo.planning.domain.ScheduleSlot;
import com.timetable.todo.planning.domain.Task;
import com.timetable.todo.planning.domain.TaskId;
import com.timetable.todo.planning.domain.WeekRange;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanningServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-31T09:00:00Z");
  private static final LocalDate MONDAY = LocalDate.of(2026, 8, 31);
  private final FakeTaskRepository tasks = new FakeTaskRepository();
  private final FakeAuditPort audits = new FakeAuditPort();
  private PlanningService service;

  @BeforeEach
  void setUp() {
    service =
        new PlanningService(
            tasks,
            audits,
            Clock.fixed(NOW, ZoneOffset.UTC),
            () -> UUID.fromString("688630db-f3a6-49a5-b7c0-7e9f6f4209a3"));
  }

  @Test
  void FR_003_create_saves_task_and_structural_audit() {
    Task created =
        service.create(
            new CreateTaskCommand(" API 계약 작성 ", null, Priority.HIGH, 60, MONDAY.plusDays(1)),
            "request-1");

    assertThat(created.title().value()).isEqualTo("API 계약 작성");
    assertThat(tasks.findById(created.id())).contains(created);
    assertThat(audits.events)
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.action()).isEqualTo(AuditAction.CREATED);
              assertThat(event.changedFields()).containsExactly("task");
              assertThat(event.requestId()).isEqualTo("request-1");
            });
  }

  @Test
  void FR_006_schedule_success_saves_and_audits() {
    Task task = storedTask("7ce92805-3426-46ee-a659-d8512ff72e19", "집중 작업", 60);

    ScheduleOutcome result =
        service.schedule(
            new ScheduleTaskCommand(task.id(), MONDAY, LocalTime.of(9, 0), task.version()),
            "request-2");

    assertThat(result).isInstanceOf(ScheduleOutcome.Scheduled.class);
    assertThat(tasks.findById(task.id()).orElseThrow().schedule()).isPresent();
    assertThat(audits.events).extracting(AuditEvent::action).containsExactly(AuditAction.SCHEDULED);
  }

  @Test
  void FR_007_conflict_returns_candidate_without_mutation_or_audit() {
    Task existing = storedTask("ad04f798-b09d-466b-ac2b-1084cc49da35", "기존 일정", 60);
    Task existingScheduled = existing.schedule(slot(MONDAY, 9, 0, 60), NOW);
    tasks.save(existingScheduled);
    Task proposed = storedTask("aaf63799-4692-43ac-b4de-ff90d3573fe6", "새 일정", 30);
    tasks.saveCalls = 0;

    ScheduleOutcome result =
        service.schedule(
            new ScheduleTaskCommand(proposed.id(), MONDAY, LocalTime.of(9, 15), proposed.version()),
            "request-3");

    assertThat(result)
        .isInstanceOfSatisfying(
            ScheduleOutcome.Conflict.class,
            conflict -> {
              assertThat(conflict.conflicting())
                  .isEqualTo(existingScheduled.schedule().orElseThrow());
              assertThat(conflict.nextCandidate()).contains(slot(MONDAY, 10, 0, 30));
            });
    assertThat(tasks.saveCalls).isZero();
    assertThat(audits.events).isEmpty();
    assertThat(tasks.findById(proposed.id()).orElseThrow().schedule()).isEmpty();
  }

  @Test
  void FR_009_repeated_completion_is_noop_without_duplicate_audit() {
    Task task = storedTask("63d84c4e-1820-46cc-9fae-b33783856ee0", "완료할 일", 30);

    Task completed =
        service.setCompletion(
            new SetCompletionCommand(task.id(), true, task.version()), "request-4");
    Task repeated =
        service.setCompletion(
            new SetCompletionCommand(task.id(), true, completed.version()), "request-5");

    assertThat(repeated).isSameAs(completed);
    assertThat(audits.events).extracting(AuditEvent::action).containsExactly(AuditAction.COMPLETED);
  }

  @Test
  void NFR_006_stale_version_never_writes() {
    Task task = storedTask("7151b211-68a6-4f36-9eb1-287f71a53da1", "최신 작업", 30);
    tasks.saveCalls = 0;

    assertThatThrownBy(
            () ->
                service.setCompletion(
                    new SetCompletionCommand(task.id(), true, task.version() + 1), "request-6"))
        .isInstanceOf(StaleTaskVersionException.class);

    assertThat(tasks.saveCalls).isZero();
    assertThat(audits.events).isEmpty();
  }

  @Test
  void FR_004_delete_requires_confirmation_and_appends_audit() {
    Task task = storedTask("750b544c-9848-45d2-8837-5668766816ba", "삭제 작업", 30);

    assertThatThrownBy(
            () ->
                service.delete(
                    new DeleteTaskCommand(task.id(), task.version(), false), "request-7"))
        .isInstanceOf(DeletionNotConfirmedException.class);

    service.delete(new DeleteTaskCommand(task.id(), task.version(), true), "request-8");

    assertThat(tasks.findById(task.id())).isEmpty();
    assertThat(audits.events).extracting(AuditEvent::action).containsExactly(AuditAction.DELETED);
  }

  @Test
  void FR_001_week_query_is_bounded_and_excludes_unscheduled_tasks() {
    Task scheduled = storedTask("e13d1735-15b5-4460-831c-0a900163f0a2", "이번 주", 30);
    tasks.save(scheduled.schedule(slot(MONDAY.plusDays(1), 8, 0, 30), NOW));
    storedTask("0c12f7c8-b82f-43d9-810f-6763e44ccbb0", "미배치", 30);

    assertThat(service.week(WeekRange.fromMonday(MONDAY)))
        .extracting(task -> task.title().value())
        .containsExactly("이번 주");
  }

  private Task storedTask(String id, String title, int estimate) {
    Task task =
        Task.create(
            new TaskId(UUID.fromString(id)), title, null, Priority.MEDIUM, estimate, null, NOW);
    tasks.save(task);
    return task;
  }

  private ScheduleSlot slot(LocalDate date, int hour, int minute, int duration) {
    return ScheduleSlot.of(
        date,
        LocalTime.of(hour, minute),
        com.timetable.todo.planning.domain.EstimateMinutes.of(duration));
  }

  private static final class FakeTaskRepository implements TaskRepositoryPort {
    private final Map<TaskId, Task> data = new LinkedHashMap<>();
    private int saveCalls;

    @Override
    public Optional<Task> findById(TaskId id) {
      return Optional.ofNullable(data.get(id));
    }

    @Override
    public Task save(Task task) {
      saveCalls++;
      data.put(task.id(), task);
      return task;
    }

    @Override
    public void delete(TaskId id) {
      data.remove(id);
    }

    @Override
    public List<Task> findScheduledIncomplete(WeekRange week) {
      return data.values().stream()
          .filter(task -> task.schedule().map(slot -> week.contains(slot.date())).orElse(false))
          .filter(task -> task.status() == com.timetable.todo.planning.domain.TaskStatus.TODO)
          .toList();
    }

    @Override
    public List<Task> findBacklog(int limit) {
      return data.values().stream()
          .filter(task -> task.status() == com.timetable.todo.planning.domain.TaskStatus.TODO)
          .filter(task -> task.schedule().isEmpty())
          .limit(limit)
          .toList();
    }

    @Override
    public TaskPage findPage(TaskListQuery query) {
      List<Task> content = data.values().stream().limit(query.size()).toList();
      return new TaskPage(
          content, query.page(), query.size(), content.size(), content.isEmpty() ? 0 : 1);
    }
  }

  private static final class FakeAuditPort implements AuditPort {
    private final List<AuditEvent> events = new ArrayList<>();

    @Override
    public void append(AuditEvent event) {
      events.add(event);
    }
  }
}
