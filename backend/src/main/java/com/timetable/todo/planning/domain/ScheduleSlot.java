package com.timetable.todo.planning.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** FR-006: Represents a valid half-open planning interval in Asia/Seoul wall time. */
public record ScheduleSlot(LocalDate date, LocalTime start, LocalTime end) {

  private static final LocalTime DAY_START = LocalTime.of(8, 0);
  private static final LocalTime DAY_END = LocalTime.of(22, 0);

  public ScheduleSlot {
    Objects.requireNonNull(date, "Schedule date is required");
    Objects.requireNonNull(start, "Schedule start is required");
    Objects.requireNonNull(end, "Schedule end is required");
    if (!isAligned(start)) {
      throw new DomainValidationException(
          "SCHEDULE_ALIGNMENT_INVALID", "Schedule start must align to 15 minutes");
    }
    if (start.isBefore(DAY_START)
        || !start.isBefore(DAY_END)
        || !end.isAfter(start)
        || end.isAfter(DAY_END)) {
      throw new DomainValidationException(
          "SCHEDULE_OUT_OF_WINDOW", "Schedule must be within the planning day");
    }
  }

  public static ScheduleSlot of(LocalDate date, LocalTime start, EstimateMinutes estimateMinutes) {
    Objects.requireNonNull(estimateMinutes, "Task estimate is required");
    return new ScheduleSlot(date, start, start.plusMinutes(estimateMinutes.value()));
  }

  public int durationMinutes() {
    return Math.toIntExact(java.time.Duration.between(start, end).toMinutes());
  }

  private static boolean isAligned(LocalTime time) {
    return time.getSecond() == 0 && time.getNano() == 0 && time.getMinute() % 15 == 0;
  }
}
