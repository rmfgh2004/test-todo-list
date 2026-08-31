package com.timetable.todo.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class SchedulePolicyPropertiesTest {

  private static final LocalDate BASE_DATE = LocalDate.of(2026, 1, 1);

  @Property(tries = 300)
  void NFR_002_PBT_02_iso_date_time_round_trip(
      @ForAll("planningDates") LocalDate date, @ForAll("quarterTimes") LocalTime time) {
    String dateValue = DateTimeFormatter.ISO_LOCAL_DATE.format(date);
    String timeValue = DateTimeFormatter.ISO_LOCAL_TIME.format(time);

    assertThat(LocalDate.parse(dateValue, DateTimeFormatter.ISO_LOCAL_DATE)).isEqualTo(date);
    assertThat(LocalTime.parse(timeValue, DateTimeFormatter.ISO_LOCAL_TIME)).isEqualTo(time);
  }

  @Property(tries = 500)
  void NFR_002_PBT_03_overlap_is_symmetric(
      @ForAll("validSlots") ScheduleSlot first, @ForAll("validSlots") ScheduleSlot second) {
    assertThat(SchedulePolicy.overlaps(first, second))
        .isEqualTo(SchedulePolicy.overlaps(second, first));
  }

  @Property(tries = 300)
  void NFR_002_PBT_03_derived_end_and_alignment_hold(@ForAll("validSlots") ScheduleSlot slot) {
    assertThat(slot.start().getMinute() % 15).isZero();
    assertThat(slot.end()).isAfter(slot.start());
    assertThat(slot.end().isAfter(LocalTime.of(22, 0))).isFalse();
    assertThat(slot.durationMinutes() % 15).isZero();
  }

  @Property(tries = 300)
  void NFR_002_PBT_03_touching_slots_never_overlap(@ForAll("touchingSlots") TouchingSlots slots) {
    assertThat(SchedulePolicy.overlaps(slots.first(), slots.second())).isFalse();
  }

  @Provide
  Arbitrary<LocalDate> planningDates() {
    return Arbitraries.integers().between(0, 364).map(BASE_DATE::plusDays);
  }

  @Provide
  Arbitrary<LocalTime> quarterTimes() {
    return Arbitraries.integers()
        .between(32, 87)
        .map(index -> LocalTime.of(index / 4, index % 4 * 15));
  }

  @Provide
  Arbitrary<ScheduleSlot> validSlots() {
    return Combinators.combine(
            planningDates(),
            Arbitraries.integers().between(1, 56),
            Arbitraries.integers().between(0, 55))
        .as(
            (date, durationQuarters, startOffset) -> {
              int boundedStart = Math.min(startOffset, 56 - durationQuarters);
              LocalTime start = LocalTime.of(8, 0).plusMinutes(boundedStart * 15L);
              return ScheduleSlot.of(date, start, EstimateMinutes.of(durationQuarters * 15));
            });
  }

  @Provide
  Arbitrary<TouchingSlots> touchingSlots() {
    return Combinators.combine(
            planningDates(),
            Arbitraries.integers().between(1, 28),
            Arbitraries.integers().between(1, 28))
        .as(
            (date, firstQuarters, secondQuarters) -> {
              int totalQuarters = firstQuarters + secondQuarters;
              int startOffset = Math.max(0, 56 - totalQuarters);
              LocalTime firstStart = LocalTime.of(8, 0).plusMinutes(startOffset * 15L);
              ScheduleSlot first =
                  ScheduleSlot.of(date, firstStart, EstimateMinutes.of(firstQuarters * 15));
              ScheduleSlot second =
                  ScheduleSlot.of(date, first.end(), EstimateMinutes.of(secondQuarters * 15));
              return new TouchingSlots(first, second);
            });
  }

  private record TouchingSlots(ScheduleSlot first, ScheduleSlot second) {}
}
