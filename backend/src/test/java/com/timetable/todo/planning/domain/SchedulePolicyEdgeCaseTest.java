package com.timetable.todo.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * FR-007, NFR-002: Boundary behaviour of the next-slot search.
 *
 * <p>Collision code carries the highest branch-coverage gate in U1 because a wrong branch here
 * silently double-books a user's day instead of failing loudly.
 */
class SchedulePolicyEdgeCaseTest {

  private static final WeekRange WEEK = WeekRange.fromMonday(LocalDate.parse("2026-09-07"));
  private static final EstimateMinutes THIRTY = EstimateMinutes.of(30);

  @Test
  void FR_007_starts_at_the_week_start_when_the_proposal_is_earlier() {
    Optional<ScheduleSlot> candidate =
        SchedulePolicy.findNextAvailable(
            WEEK, LocalDate.parse("2026-09-01"), LocalTime.of(9, 0), THIRTY, List.of());

    assertThat(candidate)
        .contains(new ScheduleSlot(WEEK.start(), LocalTime.of(8, 0), LocalTime.of(8, 30)));
  }

  @Test
  void FR_007_returns_nothing_when_the_proposal_is_after_the_week() {
    Optional<ScheduleSlot> candidate =
        SchedulePolicy.findNextAvailable(
            WEEK, LocalDate.parse("2026-09-20"), LocalTime.of(9, 0), THIRTY, List.of());

    assertThat(candidate).isEmpty();
  }

  @Test
  void FR_007_lifts_a_proposal_before_the_daily_window_to_the_window_start() {
    Optional<ScheduleSlot> candidate =
        SchedulePolicy.findNextAvailable(WEEK, WEEK.start(), LocalTime.of(6, 0), THIRTY, List.of());

    assertThat(candidate).map(ScheduleSlot::start).contains(LocalTime.of(8, 0));
  }

  @Test
  void FR_007_rounds_an_unaligned_proposal_up_to_the_next_quarter() {
    Optional<ScheduleSlot> candidate =
        SchedulePolicy.findNextAvailable(
            WEEK, WEEK.start(), LocalTime.of(9, 1, 30), THIRTY, List.of());

    assertThat(candidate).map(ScheduleSlot::start).contains(LocalTime.of(9, 15));
  }

  @Test
  void FR_007_rounds_a_late_unaligned_proposal_to_the_day_end_and_moves_on() {
    Optional<ScheduleSlot> candidate =
        SchedulePolicy.findNextAvailable(
            WEEK, WEEK.start(), LocalTime.of(23, 59, 30), THIRTY, List.of());

    assertThat(candidate)
        .contains(
            new ScheduleSlot(WEEK.start().plusDays(1), LocalTime.of(8, 0), LocalTime.of(8, 30)));
  }

  @Test
  void FR_007_returns_nothing_when_a_long_task_never_fits_before_the_day_end() {
    Optional<ScheduleSlot> candidate =
        SchedulePolicy.findNextAvailable(
            WEEK,
            WEEK.start().plusDays(6),
            LocalTime.of(21, 30),
            EstimateMinutes.of(120),
            List.of());

    assertThat(candidate).isEmpty();
  }

  @Test
  void NFR_002_overlap_requires_both_slots() {
    ScheduleSlot slot = new ScheduleSlot(WEEK.start(), LocalTime.of(9, 0), LocalTime.of(9, 30));

    assertThatThrownBy(() -> SchedulePolicy.overlaps(null, slot))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> SchedulePolicy.overlaps(slot, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void FR_007_slots_on_different_dates_never_overlap() {
    ScheduleSlot monday = new ScheduleSlot(WEEK.start(), LocalTime.of(9, 0), LocalTime.of(9, 30));
    ScheduleSlot tuesday =
        new ScheduleSlot(WEEK.start().plusDays(1), LocalTime.of(9, 0), LocalTime.of(9, 30));

    assertThat(SchedulePolicy.overlaps(monday, tuesday)).isFalse();
  }
}
