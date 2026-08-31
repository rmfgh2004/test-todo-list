package com.timetable.todo.planning.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

/** FR-001: Defines an exact Monday-based seven-day planning range. */
public record WeekRange(LocalDate start, LocalDate endExclusive) {

  public WeekRange {
    Objects.requireNonNull(start, "Week start is required");
    Objects.requireNonNull(endExclusive, "Week end is required");
    if (start.getDayOfWeek() != DayOfWeek.MONDAY || !endExclusive.equals(start.plusDays(7))) {
      throw new DomainValidationException("WEEK_START_INVALID", "Week must start on Monday");
    }
  }

  public static WeekRange fromMonday(LocalDate start) {
    Objects.requireNonNull(start, "Week start is required");
    return new WeekRange(start, start.plusDays(7));
  }

  public boolean contains(LocalDate date) {
    return !date.isBefore(start) && date.isBefore(endExclusive);
  }
}
