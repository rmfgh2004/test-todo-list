package com.timetable.todo.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskTest {

  private static final Instant NOW = Instant.parse("2026-08-31T09:00:00Z");

  @Test
  void FR_003_creates_trimmed_unscheduled_todo_task() {
    Task task =
        Task.create(
            new TaskId(UUID.fromString("43ca936f-5ba4-4e10-9f42-96afd9d89a68")),
            "  계획 검토  ",
            "설명을 확인한다",
            Priority.HIGH,
            45,
            LocalDate.of(2026, 9, 1),
            NOW);

    assertThat(task.title().value()).isEqualTo("계획 검토");
    assertThat(task.status()).isEqualTo(TaskStatus.TODO);
    assertThat(task.schedule()).isEmpty();
    assertThat(task.version()).isZero();
  }

  @Test
  void FR_003_rejects_blank_and_oversized_content() {
    assertThatThrownBy(() -> TaskTitle.of("  "))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("TASK_TITLE_INVALID");

    assertThatThrownBy(() -> TaskDescription.of("가".repeat(2_001)))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("TASK_DESCRIPTION_TOO_LONG");
  }

  @Test
  void FR_003_rejects_invalid_estimate_boundaries() {
    assertThatThrownBy(() -> EstimateMinutes.of(14))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("TASK_ESTIMATE_INVALID");
    assertThatThrownBy(() -> EstimateMinutes.of(20))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("TASK_ESTIMATE_INVALID");
    assertThat(EstimateMinutes.of(840).value()).isEqualTo(840);
  }

  @Test
  void FR_006_schedules_and_FR_008_unschedules_without_losing_content() {
    Task original = sampleTask();
    ScheduleSlot slot =
        ScheduleSlot.of(LocalDate.of(2026, 9, 1), LocalTime.of(9, 0), original.estimate());

    Task scheduled = original.schedule(slot, NOW.plusSeconds(60));
    Task unscheduled = scheduled.unschedule(NOW.plusSeconds(120));

    assertThat(scheduled.schedule()).contains(slot);
    assertThat(unscheduled.schedule()).isEmpty();
    assertThat(unscheduled.title()).isEqualTo(original.title());
    assertThat(unscheduled.version()).isEqualTo(2);
  }

  @Test
  void FR_009_completion_is_desired_state_and_idempotent() {
    Task todo = sampleTask();

    Task completed = todo.setCompleted(true, NOW.plusSeconds(60));
    Task repeated = completed.setCompleted(true, NOW.plusSeconds(120));
    Task reopened = repeated.setCompleted(false, NOW.plusSeconds(180));

    assertThat(completed.status()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(repeated).isSameAs(completed);
    assertThat(reopened.status()).isEqualTo(TaskStatus.TODO);
    assertThat(reopened.version()).isEqualTo(2);
  }

  private static Task sampleTask() {
    return Task.create(
        new TaskId(UUID.fromString("db96da58-bc1f-4c5b-b77c-5c81c0a46935")),
        "보고서 작성",
        null,
        Priority.MEDIUM,
        60,
        null,
        NOW);
  }
}
