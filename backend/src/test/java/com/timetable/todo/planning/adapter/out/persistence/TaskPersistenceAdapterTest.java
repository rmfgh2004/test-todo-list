package com.timetable.todo.planning.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.timetable.todo.planning.application.AuditAction;
import com.timetable.todo.planning.application.AuditEvent;
import com.timetable.todo.planning.application.SortDirection;
import com.timetable.todo.planning.application.StaleTaskVersionException;
import com.timetable.todo.planning.application.TaskListQuery;
import com.timetable.todo.planning.application.TaskSort;
import com.timetable.todo.planning.domain.EstimateMinutes;
import com.timetable.todo.planning.domain.Priority;
import com.timetable.todo.planning.domain.ScheduleSlot;
import com.timetable.todo.planning.domain.Task;
import com.timetable.todo.planning.domain.TaskId;
import com.timetable.todo.planning.domain.TaskStatus;
import com.timetable.todo.planning.domain.WeekRange;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TaskPersistenceAdapter.class, AuditPersistenceAdapter.class})
class TaskPersistenceAdapterTest {

  private static final Instant NOW = Instant.parse("2026-08-31T09:00:00Z");
  private static final LocalDate MONDAY = LocalDate.of(2026, 8, 31);

  @Autowired private TaskPersistenceAdapter tasks;
  @Autowired private AuditPersistenceAdapter audits;
  @Autowired private AuditEventJpaRepository auditEntities;
  @Autowired private EntityManager entityManager;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void FR_012_task_round_trip_preserves_domain_values() {
    Task original = task("5036deaa-4eaf-4b36-aec2-39ba90023d70", "DB 저장", 45);

    Task saved = tasks.save(original);
    Task loaded = tasks.findById(original.id()).orElseThrow();

    assertThat(saved).usingRecursiveComparison().isEqualTo(loaded);
    assertThat(loaded.title().value()).isEqualTo("DB 저장");
    assertThat(loaded.version()).isZero();
  }

  @Test
  void FR_006_schedule_update_and_week_query_are_bounded() {
    Task original = tasks.save(task("262ba2aa-1c12-4a2b-b0d1-8dc86af9f6fb", "배치", 30));
    ScheduleSlot slot =
        ScheduleSlot.of(MONDAY.plusDays(1), LocalTime.of(9, 0), EstimateMinutes.of(30));

    Task saved = tasks.save(original.schedule(slot, NOW.plusSeconds(60)));

    assertThat(saved.schedule()).contains(slot);
    assertThat(tasks.findScheduledIncomplete(WeekRange.fromMonday(MONDAY)))
        .extracting(Task::id)
        .containsExactly(original.id());
    assertThat(tasks.findScheduledIncomplete(WeekRange.fromMonday(MONDAY.plusWeeks(1)))).isEmpty();
  }

  @Test
  void FR_013_audit_adapter_only_appends_structural_event() {
    Task task = tasks.save(task("9fbf5137-f200-4d25-b46e-4f803361107b", "감사", 30));
    AuditEvent event =
        new AuditEvent(
            UUID.fromString("f85d045f-eb7d-4349-bebe-0d62825ee8dc"),
            task.id(),
            AuditAction.CREATED,
            "local-user",
            "request-test",
            NOW,
            List.of("task"));

    audits.append(event);

    assertThat(auditEntities.findAll())
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.getAction()).isEqualTo(AuditAction.CREATED);
              assertThat(stored.getChangedFields()).isEqualTo("task");
            });
  }

  @Test
  void NFR_006_stale_aggregate_cannot_overwrite_a_newer_version() {
    Task original = tasks.save(task("4d56e004-49ef-45e9-a08d-6243651877d6", "원본", 30));
    tasks.save(original.schedule(slot(MONDAY, 9), NOW.plusSeconds(1)));

    Task staleChange = original.schedule(slot(MONDAY, 10), NOW.plusSeconds(2));

    assertThatThrownBy(() -> tasks.save(staleChange)).isInstanceOf(StaleTaskVersionException.class);
  }

  @Test
  void FR_005_backlog_is_bounded_and_stably_orders_due_date_then_priority() {
    tasks.save(
        task(
            "25e0e095-8541-4c75-a535-8d67ed968fc6",
            "later",
            30,
            Priority.HIGH,
            MONDAY.plusDays(2)));
    tasks.save(task("495c5f9e-f582-46ca-b2aa-99a4ec893a3b", "medium", 30, Priority.MEDIUM, MONDAY));
    tasks.save(task("ad87fb89-127f-49b3-8eaf-ed834e50d94c", "high", 30, Priority.HIGH, MONDAY));

    assertThat(tasks.findBacklog(2))
        .extracting(value -> value.title().value())
        .containsExactly("high", "medium");
  }

  @Test
  void FR_010_list_filters_and_page_size_are_applied_by_the_database() {
    tasks.save(task("e1ac6c70-f507-4599-9165-c714331b015e", "Bravo", 30, Priority.HIGH, null));
    tasks.save(task("63eb3470-a604-4ace-b494-c1db28f0a4a4", "Alpha", 30, Priority.HIGH, null));
    tasks.save(task("759b221a-871f-412c-bf8c-f95dc60ef3a4", "Low", 30, Priority.LOW, null));

    var result =
        tasks.findPage(
            new TaskListQuery(
                TaskStatus.TODO, false, Priority.HIGH, TaskSort.TITLE, SortDirection.ASC, 0, 1));

    assertThat(result.content())
        .extracting(value -> value.title().value())
        .containsExactly("Alpha");
    assertThat(result.totalElements()).isEqualTo(2);
    assertThat(result.totalPages()).isEqualTo(2);
  }

  @Test
  void NFR_005_scheduled_page_avoids_per_task_schedule_queries() {
    for (int index = 0; index < 3; index++) {
      Task task =
          tasks.save(task("00000000-0000-0000-0000-00000000010" + index, "scheduled-" + index, 30));
      tasks.save(task.schedule(slot(MONDAY, 9 + index), NOW.plusSeconds(index + 1)));
    }
    entityManager.clear();
    var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    var result =
        tasks.findPage(
            new TaskListQuery(
                TaskStatus.TODO, true, null, TaskSort.CREATED_AT, SortDirection.ASC, 0, 10));

    assertThat(result.content()).hasSize(3);
    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
  }

  private Task task(String id, String title, int estimate) {
    return task(id, title, estimate, Priority.MEDIUM, null);
  }

  private Task task(String id, String title, int estimate, Priority priority, LocalDate dueDate) {
    return Task.create(
        new TaskId(UUID.fromString(id)), title, null, priority, estimate, dueDate, NOW);
  }

  private ScheduleSlot slot(LocalDate date, int hour) {
    return ScheduleSlot.of(date, LocalTime.of(hour, 0), EstimateMinutes.of(30));
  }
}
