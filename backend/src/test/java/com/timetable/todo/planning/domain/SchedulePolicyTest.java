package com.timetable.todo.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchedulePolicyTest {

  private static final LocalDate MONDAY = LocalDate.of(2026, 8, 31);

  @Test
  void FR_006_accepts_daily_boundaries_and_15_minute_alignment() {
    ScheduleSlot first = ScheduleSlot.of(MONDAY, LocalTime.of(8, 0), EstimateMinutes.of(15));
    ScheduleSlot last = ScheduleSlot.of(MONDAY, LocalTime.of(21, 45), EstimateMinutes.of(15));

    assertThat(first.end()).isEqualTo(LocalTime.of(8, 15));
    assertThat(last.end()).isEqualTo(LocalTime.of(22, 0));
  }

  @Test
  void FR_006_rejects_misaligned_or_out_of_window_slots() {
    assertThatThrownBy(() -> ScheduleSlot.of(MONDAY, LocalTime.of(8, 5), EstimateMinutes.of(30)))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("SCHEDULE_ALIGNMENT_INVALID");

    assertThatThrownBy(() -> ScheduleSlot.of(MONDAY, LocalTime.of(21, 45), EstimateMinutes.of(30)))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("SCHEDULE_OUT_OF_WINDOW");
  }

  @Test
  void FR_007_overlap_is_symmetric_and_touching_boundaries_do_not_overlap() {
    ScheduleSlot morning = slot(MONDAY, 9, 0, 60);
    ScheduleSlot overlapping = slot(MONDAY, 9, 45, 30);
    ScheduleSlot touching = slot(MONDAY, 10, 0, 30);
    ScheduleSlot tomorrow = slot(MONDAY.plusDays(1), 9, 0, 60);

    assertThat(SchedulePolicy.overlaps(morning, overlapping)).isTrue();
    assertThat(SchedulePolicy.overlaps(overlapping, morning)).isTrue();
    assertThat(SchedulePolicy.overlaps(morning, touching)).isFalse();
    assertThat(SchedulePolicy.overlaps(morning, tomorrow)).isFalse();
  }

  @Test
  void FR_007_finds_next_available_slot_from_proposal_within_selected_week() {
    List<ScheduleSlot> occupied = List.of(slot(MONDAY, 9, 0, 60), slot(MONDAY, 10, 0, 30));

    assertThat(
            SchedulePolicy.findNextAvailable(
                WeekRange.fromMonday(MONDAY),
                MONDAY,
                LocalTime.of(9, 15),
                EstimateMinutes.of(30),
                occupied))
        .contains(slot(MONDAY, 10, 30, 30));
  }

  @Test
  void FR_007_moves_search_to_next_day_and_returns_empty_after_week_exhaustion() {
    ScheduleSlot fullMonday = slot(MONDAY, 8, 0, 840);
    ScheduleSlot fullSunday = slot(MONDAY.plusDays(6), 8, 0, 840);

    assertThat(
            SchedulePolicy.findNextAvailable(
                WeekRange.fromMonday(MONDAY),
                MONDAY,
                LocalTime.of(21, 45),
                EstimateMinutes.of(30),
                List.of(fullMonday)))
        .contains(slot(MONDAY.plusDays(1), 8, 0, 30));

    assertThat(
            SchedulePolicy.findNextAvailable(
                WeekRange.fromMonday(MONDAY),
                MONDAY.plusDays(6),
                LocalTime.of(21, 45),
                EstimateMinutes.of(30),
                List.of(fullSunday)))
        .isEmpty();
  }

  @Test
  void FR_001_week_range_requires_monday() {
    assertThatThrownBy(() -> WeekRange.fromMonday(MONDAY.plusDays(1)))
        .isInstanceOf(DomainValidationException.class)
        .extracting("code")
        .isEqualTo("WEEK_START_INVALID");
  }

  private static ScheduleSlot slot(LocalDate date, int hour, int minute, int duration) {
    return ScheduleSlot.of(date, LocalTime.of(hour, minute), EstimateMinutes.of(duration));
  }
}
