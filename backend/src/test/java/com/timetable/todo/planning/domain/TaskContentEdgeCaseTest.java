package com.timetable.todo.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** FR-003, FR-004: Content validation and update boundaries of the Task aggregate. */
class TaskContentEdgeCaseTest {

  private static final Instant NOW = Instant.parse("2026-09-01T09:00:00Z");
  private static final LocalDate MONDAY = LocalDate.parse("2026-09-07");

  private Task task(int estimateMinutes) {
    return Task.create(
        new TaskId(UUID.randomUUID()),
        "content",
        null,
        Priority.MEDIUM,
        estimateMinutes,
        null,
        NOW);
  }

  @Test
  void FR_003_rejects_a_null_title() {
    assertThatThrownBy(() -> TaskTitle.of(null))
        .isInstanceOf(DomainValidationException.class)
        .satisfies(
            failure ->
                assertThat(((DomainValidationException) failure).code())
                    .isEqualTo("TASK_TITLE_INVALID"));
  }

  @Test
  void FR_003_treats_an_absent_description_as_no_description() {
    assertThat(TaskDescription.of(null)).isNull();
  }

  @Test
  void FR_004_rejects_an_update_that_moves_time_backwards() {
    Task existing = task(30);

    assertThatThrownBy(
            () -> existing.update("content", null, Priority.LOW, 30, null, NOW.minusSeconds(60)))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void FR_004_updates_content_without_touching_an_absent_placement() {
    Task updated =
        task(30).update("renamed", "detail", Priority.HIGH, 45, LocalDate.parse("2026-09-10"), NOW);

    assertThat(updated.schedule()).isEmpty();
    assertThat(updated.estimate().value()).isEqualTo(45);
    assertThat(updated.version()).isEqualTo(1);
  }

  @Test
  void FR_006_rescheduling_to_the_same_slot_is_a_no_op() {
    ScheduleSlot slot = ScheduleSlot.of(MONDAY, LocalTime.of(9, 0), EstimateMinutes.of(30));
    Task scheduled = task(30).schedule(slot, NOW);

    assertThat(scheduled.schedule(slot, NOW)).isSameAs(scheduled);
  }

  @Test
  void FR_006_rejects_a_placement_whose_length_differs_from_the_estimate() {
    ScheduleSlot mismatched = new ScheduleSlot(MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

    assertThatThrownBy(() -> task(30).schedule(mismatched, NOW))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void FR_008_unscheduling_an_unplaced_task_is_a_no_op() {
    Task unplaced = task(30);

    assertThat(unplaced.unschedule(NOW)).isSameAs(unplaced);
  }
}
